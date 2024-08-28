package com.ericsson.bss.job.cel

import javaposse.jobdsl.dsl.Job

import com.ericsson.bss.AbstractJobBuilder

class CelReleasePrepareJobBuilder extends AbstractJobBuilder{

    private String gerritName
    protected String releaseGoalLocal

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addReleasePrepareConfig()
        return job
    }

    private void addReleasePrepareConfig() {
        job.with {
            addGitRepository(gerritName)
            deleteWorkspaceBeforeBuildStarts()
            triggers { cron('H H/12 * * *') }
            steps {
                shell(cleanUpWorkspace("\${WORKSPACE}"))
                shell(removeOldArtifacts())
                shell(gitConfig("\${WORKSPACE}"))
                shell(gconfWorkspaceWorkaround())
                shell(mavenReleasePrepareComm())
            }

            injectEnv(getInjectVariables())
            addTimeoutConfig()
        }
    }

    private String mavenReleasePrepareComm(){
        String mavenReleasePrepareComm ="mvn \\\n" +
                                        releaseGoalLocal + " \\\n" +
                                        MAVEN_PARAMETERS
        return mavenReleasePrepareComm
    }
}
