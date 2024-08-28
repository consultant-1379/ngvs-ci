package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.job.washingmachine.utils.WashingMachineConstantsHelper
import com.ericsson.bss.job.washingmachine.utils.WashingMachineOnOffScriptsBuilder
import javaposse.jobdsl.dsl.Job

@Deprecated
class OldWashingMachineKeepAliveJobBuilder extends AbstractJobBuilder {

    protected static final int DAYS_TO_KEEP_BUILDS = 20
    protected static final int MAX_BUILDS_TO_KEEP = 20

    protected def out
    protected String projectName

    public Job build() {
        jobName = getJobName()
        out.println("Creating " + jobName)
        initProject(dslFactory.freeStyleJob(jobName))
        setRestrictLabel()
        job.with {
            logRotator(DAYS_TO_KEEP_BUILDS, MAX_BUILDS_TO_KEEP)

            blockOn(getProjectNameToBuild().tokenize(',')) {
                blockLevel('GLOBAL')
                scanQueueFor('ALL')
            }

            triggers {
                cron('H/30 * * * *')
            }
        }

        addTriggerParameterizedBuildOnOtherProjectsConfig()

        Map config = readConfig()
        if (config['JOB_ENABLED'] != null && !Boolean.parseBoolean(config['JOB_ENABLED'])) {
            disable()
        } else {
            enable()
        }

        return job
    }

    protected String getProjectNameToBuild() {
        return projectName + WashingMachineConstantsHelper.WASHINGMACHINE_SUFFIX
    }

    protected String getJobName() {
        return getProjectNameToBuild() + WashingMachineConstantsHelper.KEEPALIVE_SUFFIX
    }

    protected void addTriggerParameterizedBuildOnOtherProjectsConfig() {
        job.with {
            steps {
                downstreamParameterized {
                    trigger(this.getProjectNameToBuild()) {
                        parameters {
                            if (WashingMachineOnOffScriptsBuilder.ifProjectNeedParamsFromFile(getProjectNameToBuild())) {
                                propertiesFile(WashingMachineOnOffScriptsBuilder.getProjectParamsFile(getProjectNameToBuild()))
                            } else {
                                currentBuild()
                            }
                        }
                    }
                }
            }
        }
    }

    private Map readConfig() {
        Properties properties = new Properties()
        String confFile = WashingMachineOnOffScriptsBuilder.getProjectParamsFile(getProjectNameToBuild())
        FileReader fr = new FileReader(confFile)
        properties.load(fr)
        return properties
    }
}
