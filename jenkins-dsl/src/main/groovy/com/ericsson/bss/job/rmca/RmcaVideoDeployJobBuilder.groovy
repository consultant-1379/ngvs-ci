package com.ericsson.bss.job.rmca

import com.ericsson.bss.AbstractJobBuilder

import javaposse.jobdsl.dsl.Job

class RmcaVideoDeployJobBuilder extends AbstractJobBuilder {

    private String gerritName

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        timeoutForJob = 90
        addSeleniumConfig()
        addParameters()
        return job
    }

    private void addSeleniumConfig() {
        RmcaSeleniumHelper helper = new RmcaSeleniumHelper(this)
        helper.addCommonSeleniumConfig(profilesToBeUsed)
        job.with {
            label(RESTRICT_LABEL_MESOS)
            addGitRepository(gerritName, '${BRANCH_NAME}')
            concurrentBuild()
            steps {
                if (symlinkWorkspace) {
                    shell(symlinkMesosWorkSpace())
                }
                shell(gitConfig("\${WORKSPACE}"))
                shell(getGitCache("\${WORKSPACE}"))
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
                shell(helper.getSedCommand())
                shell(helper.getBuildCommand())
                shell(helper.getRunBackendCommand())
                shell(helper.getSeleniumBuildCommand())
                shell(helper.getUseCaseRecorderCommand())
                shell(helper.getBackendKillCommand())
                shell(helper.uploadVideosToARM())
            }
        }
    }

    private void addParameters() {
        job.with {
            parameters {
                stringParam('BRANCH_NAME', 'master', 'Defines the branch to build')
            }
            publishers { wsCleanup() }
        }
    }
}
