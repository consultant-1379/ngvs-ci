package com.ericsson.bss.job.rmca

import javaposse.jobdsl.dsl.Job

import com.ericsson.bss.AbstractJobBuilder

class RmcaReleasePrepareJobBuilder extends AbstractJobBuilder {

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
                shell(symlinkMesosWorkSpace())
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
                shell(removeOldArtifacts())
                shell(gitConfig("\${WORKSPACE}"))
                shell(mavenReleasePrepareComm())
            }

            injectEnv(getInjectVariables())
            addTimeoutConfig()
            customWorkspace(CUSTOM_WORKSPACE_MESOS)
            publishers {
                wsCleanup()
            }
        }
    }

    private String mavenReleasePrepareComm() {
        String mavenReleasePrepareComm = getShellCommentDescription("Limit memory, must in the same shell where test running.") +
                                        "# Sanity limit memory to 16Gb to protect from gconfd-2 memleak bug\n" +
                                        "ulimit -v 16000000\n" +
                                        "ulimit -a\n" +
                                        "cd \${WORKSPACE}\n" +
                                        getShellCommentDescription("Maven build command") +
                                        "mvn \\\n" +
                                        releaseGoalLocal + " \\\n" +
                                        MAVEN_PARAMETERS
        return mavenReleasePrepareComm
    }
}
