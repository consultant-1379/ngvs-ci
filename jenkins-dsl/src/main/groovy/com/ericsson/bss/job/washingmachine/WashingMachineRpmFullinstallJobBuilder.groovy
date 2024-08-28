package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.job.washingmachine.utils.WashingMachineConstantsHelper
import javaposse.jobdsl.dsl.Job

class WashingMachineRpmFullinstallJobBuilder extends AbstractJobBuilder {

    private static final int DAYS_TO_KEEP_BUILDS = 20
    private static final int MAX_BUILDS_TO_KEEP = 100

    private def out
    private String projectName
    private Map config

    public Job build() {
        config = readConfig()
        jobName = getJobName()
        out.println("Creating " + jobName)
        initProject(dslFactory.freeStyleJob(jobName))

        setRestrictLabel()
        discardOldBuilds(DAYS_TO_KEEP_BUILDS, MAX_BUILDS_TO_KEEP)
        buildPeriodically(config.cronExpression)
        stepTriggerBuild()
        emailNotification(config.recipients, false, false)
        postTriggerBuild()

        return job
    }

    private String getJobName() {
        return projectName + WashingMachineConstantsHelper.WASHINGMACHINE_SUFFIX + WashingMachineConstantsHelper.RPM_FULLINSTALL_SUFFIX
    }

    private void stepTriggerBuild() {
        job.with {
            steps {
                downstreamParameterized {
                    trigger(projectName + '_targethost_install') {
                        block {
                            buildStepFailure('FAILURE')
                            failure('never')
                            unstable('never')
                        }
                        parameters {
                            currentBuild()
                            predefinedProps(getParams())
                        }
                    }
                }
            }
        }
    }

    private postTriggerBuild() {
        job.with {
            publishers {
                downstreamParameterized {
                    trigger(jobName) {
                        condition('FAILED')
                        triggerWithNoParameters()
                    }
                }
            }
        }
    }

    private Map getParams() {
        Properties properties = new Properties()
        properties.load(dslFactory.streamFileFromWorkspace("properties/" + jobName + "_params.properties"))
        return properties
    }

    private Map readConfig() {
        Properties properties = new Properties()
        properties.load(dslFactory.streamFileFromWorkspace("properties/" + jobName + "_config.properties"))
        return properties
    }
}
