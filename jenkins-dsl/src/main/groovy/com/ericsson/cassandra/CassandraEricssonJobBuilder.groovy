package com.ericsson.cassandra

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

import com.ericsson.AbstractCassandraJob

class CassandraEricssonJobBuilder extends AbstractCassandraJob {

    private static final TIMEOUT = 60

    String jdk = DEFAULT_JDK

    Job build(DslFactory dslFactory) {
        dslFactory.job(name) {

            it.description this.description + CASSANDRA_TROUBLESHOOT

            label(RESTRICT_LABEL_MESOS)
            jdk(this.jdk)
            customWorkspace(this.workspace)
            logRotator(10, 10, 5, 5)

            parameters {
                booleanParam('RELEASE', false, 'Create a new relase from current commit.' +
                        '\nWill create a new release tag, and step to next developemnt version.')
            }

            wrappers {
                environmentVariables {  envs(getEnvironmentVariables())  }
                timestamps()
                timeout {
                    absolute(TIMEOUT)
                    abortBuild()
                    writeDescription('Build failed due to timeout after {0} minutes')
                }
            }

            scm {
                git this.gitUrl, this.branch
            }

            triggers { scm 'H/30 * * * *\n# Realtime pushed by the eta_gitscmpoll_trigger job' }

            steps {
                shell changeRemotesShell()
                shell cleanWorkspace()
                shell generateBuilProperties()
                shell updateBuildXml()
                shell buildAndDeploy()
                conditionalSteps{
                    condition {
                        alwaysRun()
                        status('SUCCESS', 'SUCCESS')
                    }
                    runner("Fail")
                    steps {
                        shell dslFactory.readFileFromWorkspace('scripts/cassandra_do_a_release.sh')
                    }
                }
            }
            publishers {
                archiveJunit('build/test/output/*.xml')
                downstream(getCassandraRPMJobToTrigger(), 'UNSTABLE')
            }
        }
    }

    private String updateBuildXml(){
        return "" +
                "#####################\n" +
                "#  Update build.xml #\n" +
                "#####################\n" +
                "echo \"modifying build.xml to package ericsson artifact\"\n" +
                "\n" +
                "#Not sure if we should use com.ericsson as groupId.\n" +
                "#sed -i s/apache-cassandra/ericsson-cassandra/g build.xml\n" +
                "#sed -i s/\\\"org.apache.cassandra\\\"/\\\"com.ericsson.cassandra\\\"/g build.xml\n" +
                "\n" +
                "#Change to standard maven deploy plugin, and remove specific parameters.\n" +
                "sed -i \'/maven-gpg-plugin:.*:sign-and-deploy-file/ s??maven-deploy-plugin:2.8.2:deploy-file?\' build.xml\n" +
                "sed -i \'/-DretryFailedDeploymentCount/ s?^?<\\!--?\' build.xml\n" +
                "sed -i \'/-DretryFailedDeploymentCount/ s?\$?-->?\' build.xml\n" +
                "\n" +
                "#Add our own settings.xml file.\n" +
                //    "#sed -i \'/value=\\\"-Dpackaging=\\\@{packaging}\\\"/ s?$?\n<arg\ value=\\\"-DgeneratePom=false\\\" \/\>?\'  build.xml #try to see if
                // the same pom deploy was the problem. It was not\n" +
                "sed -i \'/value=\\\"-Dpackaging=\\@{packaging}\\\"/ s?\$?\\\n<arg\\ value=\\\"--settings=\\\${maven.setting}\\\" \\/\\>?\' build.xml\n" +
                "\n" +
                "#Specify maven repository to workspace.\n" +
                "sed -i \'/value=\\\"-DpomFile=\\@{pomFile}\\\"/ s?\$?\\n<arg\\ " +
                "value=\\\"-Dmaven.repo.local=\\\${maven-ant-tasks.local}\\\"\\ \\/\\>?\' build.xml\n" +
                "\n" +
                "sed -i s/if=\\\"release\\\"//g build.xml"
    }

    private String buildAndDeploy(){
        return "" +
                "#####################\n" +
                "#  Build and Deploy #\n" +
                "#####################\n" +
                "if [ \${RELEASE} == true ] ; then \n" +
                "  #TODO: Should verify so we not building a release on the same version as before.\n" +
                "  ant publish -v -Drelease=\${RELEASE}\n" +
                "  ant test -v -Drelease=\${RELEASE} || true\n" +
                "  #echo \"nop\"\n" +
                "else\n" +
                "  ant publish -v\n" +
                "  ant test -v || true\n" +
                "fi"
    }

    private String changeRemotesShell() {
        return "" +
                "###########################\n" +
                "#  Change push to central #\n" +
                "###########################\n" +
                "git remote set-url origin ssh://gerritmirror.lmera.ericsson.se:29418/cassandra/cassandra \n" +
                "git remote set-url origin --push ssh://gerrit.ericsson.se:29418/cassandra/cassandra"
    }

    private String cleanWorkspace() {
        return "" +
                "########################\n" +
                "#  Clean up workspace  #\n" +
                "########################\n" +
                "git reset --hard HEAD\n" +
                "git clean -fdxq\n" +
                "mkdir -p \${WS_TMP}\n" +
                "mkdir -p \${MAVEN_REPOSITORY}"
    }

    private String generateBuilProperties() {
        return "" +
                "###############################\n" +
                "#  Generate build.properties  #\n" +
                "###############################\n" +
                "echo \"build.properties\"\n" +
                "\n" +
                "FILE=build.properties\n" +
                "echo \"#Download dependencies from ARM\" >> \${FILE}\n" +
                "echo \"artifact.remoteRepository.central:     https://arm.epk.ericsson.se/artifactory/proj-charging-dev\" >> \${FILE}\n" +
                "echo \"artifact.remoteRepository.java.net2:   https://arm.epk.ericsson.se/artifactory/proj-charging-dev\" >> \${FILE}\n" +
                "echo \"artifact.remoteRepository.apache:      https://arm.epk.ericsson.se/artifactory/proj-charging-dev\" >> \${FILE}\n" +
                "echo \"artifact.remoteRepository.jclouds:     https://arm.epk.ericsson.se/artifactory/proj-charging-dev\" >> \${FILE}\n" +
                "echo \"artifact.remoteRepository.oauth:       https://arm.epk.ericsson.se/artifactory/proj-charging-dev\" >> \${FILE}\n" +
                "echo \"\" >> \${FILE}\n" +
                "echo \"#Maven settings\" >> \${FILE}\n" +
                "echo \"maven-repository-id:                   arm\" >> \${FILE}\n" +
                "if [ \${RELEASE} == true ] ; then\n" +
                "  echo \"maven-repository-url:                  https://arm.epk.ericsson.se/artifactory/proj-cassandra-release-local\" >> \${FILE}\n" +
                "else\n" +
                "  echo \"maven-repository-url:                  https://arm.epk.ericsson.se/artifactory/proj-cassandra-dev-local\" >> \${FILE}\n" +
                "fi\n" +
                "echo \"maven.setting:                         \${MAVEN_SETTINGS}\" >> \${FILE}\n" +
                "echo \"#maven local reposiotry to fix                         \" >> \${FILE}\n" +
                "echo \"\" >> \${FILE}\n" +
                "echo \"#SCM settings\" >> \${FILE}\n" +
                "echo \"scm.connection:                        scm:ssh://gerrit.ericsson.se:29418/cassandra/cassandra.git\" >> \${FILE}\n" +
                "echo \"scm.developerConnection:               scm:ssh://gerrit.ericsson.se:29418/cassandra/cassandra.git\" >> \${FILE}\n" +
                "echo \"\" >> \${FILE}\n" +
                "echo \"#SCM settings\" >> \${FILE}\n" +
                "echo \"maven-ant-tasks.local:                 \${MAVEN_REPOSITORY}\"                                  >> \${FILE}"
    }

    private static final MAVEN_OPTS = "-server -Xss1M -Xms128M -Xmx4G -XX:MaxPermSize=128M -verbose:gc -Djava.io.tmpdir=\${WS_TMP}"
    private static final MAVEN_HOME = "/opt/local/dev_tools/maven/apache-maven-3.3.9"

    private getEnvironmentVariables() {
        def env_list = [:]

        env_list.put("WS_TMP", "\${WORKSPACE}/.tmp/")
        env_list.put("MAVEN_REPOSITORY", "\${WORKSPACE}/.repository")
        env_list.put("MAVEN_SETTINGS", MAVEN_SETTINGS)
        env_list.put("M2_HOME", MAVEN_HOME)
        env_list.put("M2", "\${M2_HOME}/bin")
        env_list.put("MAVEN_OPTS", MAVEN_OPTS)
        env_list.put("JAVA_TOOL_OPTIONS", "-Xmx4G -XX:SelfDestructTimer=" + TIMEOUT)
        env_list.put("ANT_HOME", '/opt/local/dev_tools/ant/latest/')
        env_list.put("PATH", "\${ANT_HOME}/bin:\${M2}:\${PATH}")
        env_list.put("LC_ALL", "en_US.UTF-8")
        env_list.put("LANG", "en_US.UTF-8")

        return env_list
    }
}
