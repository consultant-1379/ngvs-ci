package com.ericsson.bss.job

public class NpmDeployReleaseFromMasterJobBuilder extends NpmDeployJobBuilder {

    protected String snapshotDeployJobName
    protected final static String JOB_DESCRIPTION = "This job is used to perform major " +
            "and minor releases from the master branch.\n" +
            "To perform a patch release the user should select a subfolder with " +
            "the corresponding branch name in the previous folder.\n" +
            "If the subfolder with the corresponding branch name is not present on the list,\n" +
            "the user has to create this branch first using \'create_patch_branch\' job." +
            "For the detailed description of the deployment process see \n" +
            "<a href=\"https://eta.epk.ericsson.se/wiki/index.php5/NPM_release_process\" " +
            "target=\"_blank\">NPM release process</a> wiki page.\n"

    NpmDeployReleaseFromMasterJobBuilder() {
        branchName = "master"
        snapshotDeployJobName = ""
    }

    @Override
    protected void addDeployConfig() {
        job.with {
            description(DSL_DESCRIPTION + JOB_DESCRIPTION)
            addGitRepository(gerritName, branchName)
            getDeployConfig()
            addBlameMail()

            publishers { wsCleanup() }
        }
    }

    @Override
    protected getInjectVariables() {
        Map envList = super.getInjectVariables()

        envList.put("RELEASE_TAG", releaseTag)
        envList.put("RELEASE_STEP", "minor")

        return envList
    }

    protected addNpmDeploy() {
        job.with {
            steps {
                parameters {
                    choiceParam('RELEASE_TYPE',
                                ['minor', 'major'],
                                dslFactory.readFileFromWorkspace("html/npm_deploy_release_type_parameter_description.html")
                    )

                    booleanParam('DRY_RUN', true,
                                 '<p>\n' +
                                 '  If this option is selected: \n' +
                                 '  <ul>\n' +
                                 '    <li> <b>no changes</b> will be pushed to git ' +
                                 'repository; </li>\n' +
                                 '    <li> <b>no artifacts</b> will be published to ' +
                                 'private registry in ARM; </li> \n' +
                                 '  </ul>\n' +
                                 '</p>')
                }

                addPreBuildSteps()

                groovyCommand(readAndVerifyVersion(), GROOVY_INSTALLATION_NAME)
                groovyCommand(verifyNpmRegistry(), GROOVY_INSTALLATION_NAME)
                shell(setNpmRegistry() )

                // trigger the test job if it is defined
                if ("" != testJobName) {
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

                // trigger the code analysis job if it is defined
                if ("" != codeAnalysisJobName) {
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

                shell(installNpmSnapshotModule() )
                shell(installNpmVersionModule() )
                shell(deploy() )

                // trigger the job, which will deploy a snapshot, if it is defined
                if ("" != snapshotDeployJobName) {
                    downstreamParameterized {
                        trigger(snapshotDeployJobName) {
                            block {
                                buildStepFailure('FAILURE')
                                failure('FAILURE')
                                unstable('UNSTABLE')
                            }
                            parameters {
                                currentBuild()
                                booleanParam("RUN_TESTS", false)
                            }
                        }
                    }
                }
            }
        }
    }

    protected String deploy() {
        return dslFactory.readFileFromWorkspace("scripts/npm_deploy_release.sh")
    }
}
