package com.ericsson.javadriver

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

import com.ericsson.AbstractCassandraJob

class JavaDriverCreateEricssonBranchJobBuilder extends AbstractCassandraJob {

    private static final TIMEOUT = 15
    private static final MAVEN_OPTS = "-server -Xss1M -Xms128M -Xmx1G -XX:MaxPermSize=128M -verbose:gc -Djava.io.tmpdir=\${WS_TMP}"
    private static final MAVEN_HOME = "/opt/local/dev_tools/maven/apache-maven-3.3.9"

    private String createBranchScriptFile = 'scripts/javadriver_create_ericsson_branch.sh'

    private DslFactory dslFactory

    String jdk = DEFAULT_JDK

    Job build(DslFactory dslFactory) {
        this.dslFactory = dslFactory

        dslFactory.job(name) {

            it.description this.description + CASSANDRA_TROUBLESHOOT

            label(RESTRICT_LABEL_MESOS)

            customWorkspace(this.workspace)

            jdk(this.jdk)

            logRotator(10, 10, 5, 5)

            parameters {
                activeChoiceParam('GIT_TAG') {
                    description("Tag to create release branch from.")
                    choiceType('SINGLE_SELECT')
                    groovyScript {
                        script(getJavaDriverGitTags())
                        fallbackScript('return ["Error evaluating Groovy script.", ""]')
                    }
                }
            }

            wrappers {
                environmentVariables {
                    envs(getEnvironmentVariables())
                }
                timestamps()
                timeout {
                    absolute(TIMEOUT)
                    abortBuild()
                    writeDescription('Build failed due to timeout after {0} minutes')
                }
            }

            steps {
                shell initGitRepository()
                shell cleanUpWorkspace()
                shell removeOldArtifacts()
                shell getCreateBranchScript()
                shell getMavenCommand()
                shell getGitPushCommand()
            }

            publishers {
                wsCleanup()
            }
        }
    }

    private String initGitRepository() {
        return "##############\n" +
        "# Init clone #\n" +
        "##############\n" +
        "PROJECT=cassandra/java-driver\n" +
        "git clone ssh://gerritmirror.lmera.ericsson.se:29418/\${PROJECT} .\n" +
        "git remote set-url origin --push ssh://gerrit.ericsson.se:29418/\${PROJECT}"
    }

    private String cleanUpWorkspace() {
        return "" +
                "########################\n" +
                "#  Clean up workspace  #\n" +
                "########################\n" +
                "git clean -fdxq\n" +
                "mkdir -p \${WS_TMP}"
    }

    private String removeOldArtifacts() {
        return "" +
                "##########################\n" +
                "#  Remove old artifacts  #\n" +
                "##########################\n" +
                "mkdir -p \${MAVEN_REPOSITORY}/com/ericsson\n" +
                "find \${MAVEN_REPOSITORY}/com/ericsson -name '*-SNAPSHOT' -type d -print0 | xargs -0 rm -rf\n" +
                "find \${MAVEN_REPOSITORY} -type f -mmin +1440 -delete -o -name '*-SNAPSHOT' -type f -delete -o -name '*lastUpdated' -type f -delete -o -type d -empty -delete"
    }

    private getEnvironmentVariables() {
        def environmentList = [:]

        environmentList.put("NEW_BRANCH_NAME", "ericsson-\${GIT_TAG}")
        environmentList.put("NEW_VERSION", "\${GIT_TAG}-E000-SNAPSHOT")
        environmentList.put("WS_TMP", "\${WORKSPACE}/.tmp/")
        environmentList.put("MAVEN_REPOSITORY", "\${WORKSPACE}/.repository")
        environmentList.put("MAVEN_SETTINGS", MAVEN_SETTINGS)
        environmentList.put("M2_HOME", MAVEN_HOME)
        environmentList.put("M2", "\${M2_HOME}/bin")
        environmentList.put("MAVEN_OPTS", MAVEN_OPTS)
        environmentList.put("JAVA_TOOL_OPTIONS", "-Xms128M -Xmx1G -verbose:gc -XX:SelfDestructTimer=" + TIMEOUT + " -Djava.io.tmpdir=\${WS_TMP}")
        environmentList.put("PATH", "\${M2}:\${PATH}")
        environmentList.put("LC_ALL", "en_US.UTF-8")
        environmentList.put("LANG", "en_US.UTF-8")

        return environmentList
    }

    private String getCreateBranchScript(){
        if (!createBranchScriptFile.equals("")) {
            return dslFactory.readFileFromWorkspace(createBranchScriptFile)
        }
        else {
            return ""
        }
    }

    private String getMavenCommand() {
        return "" +
                "#####################\n" +
                "#  Set new version  #\n" +
                "#####################\n" +
                "if [ ! -z \${GIT_TAG} ] ; then\n" +
                "  mvn versions:set -DnewVersion=\${GIT_TAG}-E000-SNAPSHOT --settings \${MAVEN_SETTINGS} -Dmaven.repo.local=\${MAVEN_REPOSITORY} -Djava.io.tmpdir=\${WS_TMP}\n" +
                "fi"
    }

    private String getGitPushCommand() {
        return "" +
                "#######################\n" +
                "#  Create new branch  #\n" +
                "#######################\n" +
                "if [ ! -z \${GIT_TAG} ] ; then\n" +
                "  git add -u\n" +
                "  git commit -m \"Update versions for \${NEW_VERSION}\"\n" +
                "  git push origin HEAD:refs/heads/\${NEW_BRANCH_NAME}\n" +
                "fi"
    }

    private String getJavaDriverGitTags(){
        return "" +
                "def version_list = []\n" +
                "String git_repo = \"" + this.gitUrl + "\"\n" +
                "String p4 = \"/opt/local/dev_tools/git/latest/bin/git ls-remote --tags \" + git_repo + \" | awk -F ' ' '{print \\\$2}' | grep -P '(?<!\\\\^{})\\\$' | sort -rV\"\n" +
                "def output = ['bash', '-c', p4].execute().in.text\n" +
                "output.tokenize('\\n').each {\n" +
                "  version_list.add(it[10..-1])\n" +
                "}\n" +
                "return version_list"
    }
}
