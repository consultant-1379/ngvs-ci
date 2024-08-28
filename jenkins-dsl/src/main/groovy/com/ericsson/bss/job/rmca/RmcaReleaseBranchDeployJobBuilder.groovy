package com.ericsson.bss.job.rmca

import com.ericsson.bss.job.DeployJobBuilder

public class RmcaReleaseBranchDeployJobBuilder extends DeployJobBuilder {

    @Override
    public void buildReleaseBranch(String branchName) {
        branch = branchName
        initProject(dslFactory.mavenJob(jobName))
        addReleaseConfig()
        addReleaseBranchDeployConfig()
    }

    public addReleaseBranchDeployConfig() {
        job.with {
            parameters {
                booleanParam('DEPLOY_VIDEOS', false, 'If set, use case test videos will be deployed to ARM')
            }
            postBuildSteps {
                conditionalSteps {
                    condition{
                        booleanCondition('\${DEPLOY_VIDEOS}')
                    }
                    runner('DontRun')
                    steps {
                        downstreamParameterized {
                            trigger('/rmca/rmca/rmca_video_deploy') {
                                parameters {
                                    predefinedProp('BRANCH_NAME', branch)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
