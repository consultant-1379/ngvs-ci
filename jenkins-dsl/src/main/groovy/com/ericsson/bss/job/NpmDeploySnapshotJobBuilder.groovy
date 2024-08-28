package com.ericsson.bss.job

import com.ericsson.bss.util.GitUtil

public class NpmDeploySnapshotJobBuilder extends NpmDeployJobBuilder {

    protected final static String JOB_DESCRIPTION = "This job is used to perform " +
            "snapshot deployment.\n For the detailed description of the deployment process see \n" +
            "<a href=\"https://eta.epk.ericsson.se/wiki/index.php5/NPM_release_process\" " +
            "target=\"_blank\">NPM release process</a> wiki page.\n"

    NpmDeploySnapshotJobBuilder() {
        branchName = "master"
        snapshotTag = "SNAPSHOT"
    }

    @Override
    protected void addDeployConfig() {
        job.with {
            description(DSL_DESCRIPTION + JOB_DESCRIPTION)
            addGitRepository(gerritName, branchName)
            triggers {
                if (GitUtil.isLocatedInGitolite(gerritName)) {
                    scm(SCM_POLLING_FREQUENT)
                } else if (gerritServer.equalsIgnoreCase(GitUtil.GERRIT_CENTRAL_SERVER)) {
                    scm('H/5 * * * *')
                } else {
                    scm(SCM_POLLING + '\n# Realtime pushed by the eta_gitscmpoll_trigger job')
                }
            }
            getDeployConfig()
            addBlameMail()

            publishers { wsCleanup() }
        }
    }

    protected addNpmDeploy() {
        job.with {
            steps {
                parameters {
                    booleanParam('DRY_RUN', false,
                                 '<p>\n' +
                                 '  If this option is selected: \n' +
                                 '  <ul>\n' +
                                 '    <li> <b>no changes</b> will be pushed to git ' +
                                 'repository; </li>\n' +
                                 '    <li> <b>no artifacts</b> will be published to ' +
                                 'private registry in ARM; </li> \n' +
                                 '  </ul>\n' +
                                 '</p>')

                    booleanParam('RUN_TESTS', true,
                                 'If true -- tests will be triggered during the ' +
                                         'snapshot deployment process')
                    }

                addPreBuildSteps()

                groovyCommand(readAndVerifyVersion(), GROOVY_INSTALLATION_NAME)
                groovyCommand(verifyNpmRegistry(), GROOVY_INSTALLATION_NAME)
                shell(setNpmRegistry() )

                // trigger the test job if it is defined
                if ("" != testJobName) {
                    conditionalSteps {
                        condition {
                            booleanCondition('\${RUN_TESTS}')
                        }
                        steps {
                            downstreamParameterized {
                                trigger(testJobName) {
                                    block {
                                        buildStepFailure('FAILURE')
                                        failure('FAILURE')
                                        unstable('UNSTABLE')
                                    }
                                }
                            }
                        }
                    }
                }

                // trigger the code analysis job if it is defined
                if ("" != codeAnalysisJobName) {
                    conditionalSteps {
                        condition {
                            booleanCondition('\${RUN_CODE_ANALYSIS}')
                        }
                        steps {
                            downstreamParameterized {
                                trigger(codeAnalysisJobName) {
                                    block {
                                        buildStepFailure('FAILURE')
                                        failure('FAILURE')
                                        unstable('UNSTABLE')
                                    }
                                }
                            }
                        }
                    }
                }

                shell(installNpmSnapshotModule() )
                shell(installNpmVersionModule() )
                shell(deploy() )
            }
        }
    }

    protected String deploy() {
        return dslFactory.readFileFromWorkspace("scripts/npm_deploy_snapshot.sh")
    }
}
