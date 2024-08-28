package com.ericsson.cassandra

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

import com.ericsson.AbstractCassandraJob

class CassandraGerritJobBuilder extends AbstractCassandraJob {

    String gerritServer = 'gerrit.ericsson.se'

    String gerritServerMirror = 'gerritmirror.lmera.ericsson.se'

    String gerritUser = 'kascmadm'

    String projectName = 'cassandra/cassandra'

    private static final TIMEOUT = 30

    String jdk = DEFAULT_JDK

    List branchesToBeTriggered

    Job build(DslFactory dslFactory) {
        dslFactory.job(name) {

            it.description this.description + CASSANDRA_TROUBLESHOOT

            label(RESTRICT_LABEL_MESOS)

            jdk(this.jdk)

            customWorkspace(this.workspace)

            concurrentBuild(true)

            logRotator(10, 10, 5, 5)

            triggers {
                gerrit {
                    project(projectName, branchesToBeTriggered)
                    configure {
                        (it / 'silentMode').setValue('true')
                        (it / 'serverName').setValue(gerritServer)
                        it / triggerOnEvents {
                            'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.events.PluginPatchsetCreatedEvent' {
                                excludeDrafts('true')
                                excludeNoCodeChange('true')
                            }
                            'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.events.PluginDraftPublishedEvent' {
                            }
                        }
                    }
                }
            }

            steps {
                shell gerritReviewStarted()
                shell gerritInitRepository()
                shell gitConfig()
                shell gitFetchGerritChange()
                shell cleanWorkspace()
                shell removeOldArtifacts()
                shell dslFactory.readFileFromWorkspace('scripts/cassandra_update_build_properties.sh')
                shell buildAndTest()
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

                flexiblePublish {
                    conditionalAction {
                        condition { status('SUCCESS', 'SUCCESS') }
                        steps { shell(unitTestReviewSuccess()) }
                    }
                }
                flexiblePublish {
                    conditionalAction {
                        condition { status('ABORTED', 'FAILURE') }
                        steps { shell(unitTestReviewFail()) }
                    }
                }
                flexiblePublish {
                    conditionalAction {
                        condition { status('UNSTABLE', 'UNSTABLE') }
                        steps { shell(unitTestReviewFail()) }
                    }
                }
            }
        }
    }

    private String unitTestReviewSuccess() {
        return "GERRIT_USER=\"" + gerritUser + "\"\n" +
                "ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT}  -m '\"UnitTest SUCCESSFUL, '\${BUILD_URL}'\"' -l Verified=1 \${GERRIT_PATCHSET_REVISION}\n"
    }

    private String unitTestReviewFail() {
        return "GERRIT_USER=\"" + gerritUser + "\"\n"+
                "ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT}  -m '\"UnitTest FAILED, '\${BUILD_URL}'\"' -l Verified=0 \${GERRIT_PATCHSET_REVISION}\n"
    }

    private String buildAndTest(){
        return "" +
                "#####################\n" +
                "#  Build and Test #\n" +
                "#####################\n" +
                "ant test -v"
    }

    private static final REMOVE_OLD_ARTIFACTS_CMD = "mkdir -p \${MAVEN_REPOSITORY}/com/ericsson\n" +
            "find \${MAVEN_REPOSITORY}/com/ericsson -name '*-SNAPSHOT' -type d -print0 | xargs -0 rm -rf\n" +
            "find \${MAVEN_REPOSITORY} -type f -mmin +1440 -delete -o -name '*-SNAPSHOT' -type f -delete -o " +
            "-name '*lastUpdated' -type f -delete -o -type d -empty -delete"

    private String removeOldArtifacts() {
        return "" +
                "##########################\n" +
                "#  Remove old artifacts  #\n" +
                "##########################\n" +
                REMOVE_OLD_ARTIFACTS_CMD
    }

    private String gitFetchGerritChange() {
        return "" +
                "############################\n" +
                "#  Git Fetch Gerrit Change #\n" +
                "############################\n" +
                "git -c gc.auto=10000 fetch ssh://" + gerritUser + "@" + gerritServerMirror + ":29418/\${GERRIT_PROJECT} \${GERRIT_REFSPEC}\n" +
                "git reset --hard \${GERRIT_PATCHSET_REVISION}"
    }

    private String cleanWorkspace() {
        return "" +
                "########################\n" +
                "#  Clean up workspace  #\n" +
                "########################\n" +
                "git clean -fdxq\n" +
                "mkdir -p \${WS_TMP}\n" +
                "mkdir -p \${MAVEN_REPOSITORY}"
    }

    private String gitConfig(){
        return "" +
                "################\n" +
                "#  Git Config  #\n" +
                "################\n" +
                "printf \"\${GIT_CLONE_CACHE}/.git/objects\" > .git/objects/info/alternates\n" +
                "git config --replace-all gc.reflogexpireunreachable '2 days ago'\n" +
                "git config --replace-all gc.reflogexpire '2 days ago'\n" +
                "git config --replace-all gc.pruneexpire '2 days ago'\n" +
                "git gc --auto"
    }

    private String gerritInitRepository() {
        return "" +
                "#############################\n" +
                "#  Git Initiate Repository  #\n" +
                "#############################\n" +
                "test -f \${WORKSPACE}/.git || git init \${WORKSPACE}"
    }

    private String gerritReviewStarted() {
        return "" +
                "###########################\n" +
                "#  Gerrit review started  #\n" +
                "###########################\n" +
                "ssh -o BatchMode=yes -p 29418 -l " + gerritUser + " " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT}  -m \'\"Unit tests started, \'\${BUILD_URL}\' \"\' \${GERRIT_PATCHSET_REVISION}"
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
        env_list.put("ANT_HOME", '/opt/local/dev_tools/ant/latest')
        env_list.put("GIT_HOME", '/opt/local/dev_tools/git/latest')
        env_list.put("GIT_CLONE_CACHE", '/workarea/bss-f_gen/kascmadm/.gitclonecache')
        env_list.put("PATH", "\${GIT_HOME}/bin:\${ANT_HOME}/bin:\${M2}:\${PATH}")
        env_list.put("LC_ALL", "en_US.UTF-8")
        env_list.put("LANG", "en_US.UTF-8")

        return env_list
    }
}
