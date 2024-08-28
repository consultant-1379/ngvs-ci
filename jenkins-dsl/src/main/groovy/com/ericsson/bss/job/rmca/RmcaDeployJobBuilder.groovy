package com.ericsson.bss.job.rmca

import com.ericsson.bss.job.DeployJobBuilder

class RmcaDeployJobBuilder extends DeployJobBuilder {
    protected getDeployConfig() {
        Closure preSteps = {
            shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
            shell(removeOldArtifacts())
            shell(gitConfig("\${WORKSPACE}"))
            if (generateGUIconfig) {
                shell(gconfWorkspaceWorkaround())
            }
        }

        Map environmentVariables = getInjectVariables()

        job.with {
            parameters {
                booleanParam('DEPLOY_VIDEOS', false, 'If set, use case test videos will be deployed to ARM')
            }
            preBuildSteps(preSteps)
            postBuildSteps {
                conditionalSteps {
                    condition{
                        booleanCondition('\${DEPLOY_VIDEOS}')
                    }
                    runner('DontRun')
                    steps {
                        downstreamParameterized {
                            trigger('/rmca/rmca/rmca_video_deploy')
                        }
                    }

                }
            }

            injectEnv(environmentVariables)

            addMavenConfig()
            addMavenRelease()
            addTimeoutConfig()
        }
    }
}
