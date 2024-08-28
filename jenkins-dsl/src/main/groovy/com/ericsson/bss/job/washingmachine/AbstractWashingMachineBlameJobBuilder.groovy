package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.job.washingmachine.utils.WashingMachineConstantsHelper
import javaposse.jobdsl.dsl.Job

abstract class AbstractWashingMachineBlameJobBuilder extends AbstractJobBuilder {

    private static final int DAYS_TO_KEEP_BUILDS = 20
    private static final int MAX_BUILDS_TO_KEEP = 20

    protected abstract void prepareParameters()
    protected abstract String getIncludedPatterns()

    protected def out
    protected String projectName

    public Job build() {
        jobName = getJobName()
        out.println("Creating " + jobName)
        initProject(dslFactory.freeStyleJob(jobName))

        job.with {
            logRotator(DAYS_TO_KEEP_BUILDS, MAX_BUILDS_TO_KEEP)
            concurrentBuild()
        }

        prepareParameters()
        deleteWorkspaceBeforeBuildStarts()
        addTimeoutAndAbortConfig(60)
        setJenkinsUserBuildVariables()
        copyArtifacts()
        addScriptBuildStep()

        return job
    }

    protected String getJobName() {
        return projectName + WashingMachineConstantsHelper.WASHINGMACHINE_SUFFIX + WashingMachineConstantsHelper.BLAME_SUFFIX
    }

    protected void copyArtifacts() {
        job.with {
            steps {
                copyArtifacts('${UPSTREAM_JOB}') {
                    buildSelector {
                        upstreamBuild()
                    }
                    includePatterns(getIncludedPatterns())
                    fingerprintArtifacts(true)
                    excludePatterns()
                    buildSelector()
                    targetDirectory()
                }
            }
        }
    }

    protected String getBuildScriptName() {
        return "scripts/washingmachine/" + jobName + ".sh"
    }

    private void addScriptBuildStep() {
        job.with {
            steps {
                shell(dslFactory.readFileFromWorkspace(getBuildScriptName()))
            }
        }
    }
}
