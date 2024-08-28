package com.ericsson.javadriver

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

import com.ericsson.AbstractCassandraJob

class JavaDriverEricssonJobBuilder extends AbstractCassandraJob {

    private static final TIMEOUT = 15

    String jdk = DEFAULT_JDK

    Job build(DslFactory dslFactory) {
        dslFactory.mavenJob(name) {

            it.description this.description + '\n' +
                    '<br/>\n' +
                    '<dl>\n' +
                    '<dt>Prerequisites</dt>\n' +
                    '<dd>- Release plugins overridden to skip gpg signing. See commit d0f5c38</dd>\n' +
                    '<dd>- SCM tag needs to be updated to internal repository. See commit 647aa68</dd>\n' +
                    '</dl>' + CASSANDRA_TROUBLESHOOT

            label(RESTRICT_LABEL_MESOS)

            jdk(this.jdk)

            logRotator(10, 10, 5, 5)

            String extraReleaseParameters = ' -Darguments=\"-DaltDeploymentRepository=arm::default::https://' +
                    'arm.epk.ericsson.se/artifactory/proj-cassandra-release-local -Dmaven.repo.local=\${MAVEN_REPOSITORY} ' +
                    '-Djava.io.tmpdir=\${WORKSPACE}/.tmp/\" --settings \${MAVEN_SETTINGS}'

            wrappers {
                environmentVariables {  envs(getEnvironmentVariables())  }
                timestamps()
                timeout {
                    absolute(TIMEOUT)
                    abortBuild()
                    writeDescription('Build failed due to timeout after {0} minutes')
                }
                mavenRelease {
                    releaseGoals('-Dresume=false release:prepare release:perform' + extraReleaseParameters)
                    dryRunGoals('-Dresume=false -DdryRun=true release:prepare' + extraReleaseParameters)
                    numberOfReleaseBuildsToKeep(-1)
                }
            }

            scm {
                git{
                    remote { url(this.gitUrl) }
                    branch (this.branch)
                    extensions {
                        localBranch (this.branch.replace('origin/', '') )
                    }
                }
            }

            triggers { scm 'H/30 * * * *\n# Realtime pushed by the eta_gitscmpoll_trigger job' }

            preBuildSteps {
                shell cleanUpWorkspace()
                shell removeOldArtifacts()
            }

            mavenInstallation('Maven 3.3.9')
            rootPOM("pom.xml")
            def mvnGoals = 'clean deploy -U -B -e ' +
                    '-DaltDeploymentRepository=arm::default::https://arm.epk.ericsson.se/artifactory/proj-cassandra-dev-local ' +
                    '-DaltReleaseDeploymentRepository=arm::default::https://arm.epk.ericsson.se/artifactory/proj-cassandra-release-local ' +
                    '-DaltSnapshotDeploymentRepository=arm::default::https://arm.epk.ericsson.se/artifactory/proj-cassandra-dev-local ' +
                    '-Dsurefire.useFile=false ' +
                    '--settings \${MAVEN_SETTINGS} ' +
                    '-Dmaven.repo.local=\${MAVEN_REPOSITORY} ' +
                    '-Djava.io.tmpdir=\${WORKSPACE}/.tmp/'

            goals(mvnGoals)
            mavenOpts("\${MAVEN_OPTS}")
            localRepository(LocalRepositoryLocation.LOCAL_TO_WORKSPACE)
            archivingDisabled(true)

            configure { project ->
                project / settings(class: 'jenkins.mvn.FilePathSettingsProvider') { path("\${MAVEN_SETTINGS}") }
            }

            customWorkspace(this.workspace)
        }
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
                "find \${MAVEN_REPOSITORY} -type f -mmin +1440 -delete -o -name '*-SNAPSHOT' -type f -delete -o -name '*lastUpdated' " +
                "-type f -delete -o -type d -empty -delete"
    }

    private static final MAVEN_OPTS = "-server -Xss1M -Xms128M -Xmx1G -XX:MaxPermSize=128M -verbose:gc -Djava.io.tmpdir=\${WS_TMP}"
    private static final MAVEN_HOME = "/opt/local/dev_tools/maven/apache-maven-3.3.9"

    private getEnvironmentVariables() {
        def env_list = [:]

        env_list.put("WS_TMP", "\${WORKSPACE}/.tmp/")
        env_list.put("MAVEN_REPOSITORY", "\${WORKSPACE}/.repository")
        env_list.put("MAVEN_SETTINGS", MAVEN_SETTINGS)
        env_list.put("M2_HOME", MAVEN_HOME)
        env_list.put("M2", "\${M2_HOME}/bin")
        env_list.put("MAVEN_OPTS", MAVEN_OPTS)
        env_list.put("JAVA_TOOL_OPTIONS", "-Xms128M -Xmx1G -verbose:gc -XX:SelfDestructTimer=" + TIMEOUT + " -Djava.io.tmpdir=\${WS_TMP}")
        env_list.put("PATH", "\${M2}:\${PATH}")
        env_list.put("LC_ALL", "en_US.UTF-8")
        env_list.put("LANG", "en_US.UTF-8")

        return env_list
    }
}
