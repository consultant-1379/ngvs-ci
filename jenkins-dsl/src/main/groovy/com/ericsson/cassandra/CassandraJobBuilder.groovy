package com.ericsson.cassandra

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

import com.ericsson.AbstractCassandraJob

class CassandraJobBuilder extends AbstractCassandraJob {

    private static final TIMEOUT = 90

    String jdk = DEFAULT_JDK

    Job build(DslFactory dslFactory) {
        dslFactory.job(name) {

            it.description this.description + CASSANDRA_TROUBLESHOOT

            label(RESTRICT_LABEL_MESOS)

            jdk(this.jdk)

            customWorkspace(this.workspace)

            logRotator(10, 10, 5, 5)

            scm {
                git this.gitUrl, this.branch
            }

            triggers { scm 'H/30 * * * *\n# Realtime pushed by the eta_gitscmpoll_trigger job' }

            steps {
                shell changeRemotesShell()
                shell cleanWorkspace()
                shell generateBuilProperties()
                shell buildAndDeploy()
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

            publishers {
                archiveJunit('build/test/output/*.xml')
                downstream(getCassandraRPMJobToTrigger(), 'UNSTABLE')
            }
        }
    }

    private String buildAndDeploy(){
        return "" +
                "#####################\n" +
                "#  Build and Deploy #\n" +
                "#####################\n" +
                "ant publish -v\n" +
                "ant test -v || true\n"
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
                "echo \"maven-repository-url:                  https://arm.epk.ericsson.se/artifactory/proj-cassandra-dev-local\" >> \${FILE}\n" +
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
