package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractJobBuilder
import javaposse.jobdsl.dsl.Job

class WashingMachineUpgradeKeepAliveJobBuilder extends AbstractJobBuilder {
    protected out
    protected projectName
    protected boolean runXvfb = false

    protected String schema // 'H/30 * * * *'
    protected ArrayList blockingJobs
    protected String fileName
    protected String suffix

    public Job build() {
        jobName = projectName + '_washingmachine' + suffix + '_keepalive'
        out.println("Creating washingmachine keepalive job for " + projectName + '_washingmachine' + suffix)
        initProject(dslFactory.freeStyleJob(jobName))
        setProjectDescription(getJobDescription())
        setRestrictLabel(RESTRICT_LABEL_MESOS_LIGHT)
        discardOldBuilds(20, 20)
        quietPeriod()
        buildPeriodically(schema)
        blockBuildIfJobsAreRunning(
            projectName + '_washingmachine' + suffix,
            'GLOBAL',
            'ALL'
        )
        deleteWorkspaceBeforeBuildStarts()
        deleteWorkspaceAfterBuild()

        setDefaultOnOffState()
        addBuildSteps()

        return job
    }

    protected String getJobDescription() {
        return "Keepalive job for ${projectName}_washingmachine${suffix}"
    }

    protected void setDefaultOnOffState() {
        Map properties = getPropertiesFromFile(fileName)
        if (properties['JOB_ENABLED'] != null && !Boolean.parseBoolean(properties['JOB_ENABLED'])) {
            disable()
        } else {
            enable()
        }
    }

    protected void quietPeriod(Integer period = 20) {
        job.with { quietPeriod(period) }
    }

    @SuppressWarnings('JavaIoPackageAccess')
    private Map getPropertiesFromFile(String fileName) {
        checkIfFileExists(fileName)
        Properties properties = new Properties()
        FileReader fr = new FileReader(fileName) //NOPMD
        properties.load(fr)
        fr.close()
        return properties
    }

    @SuppressWarnings('JavaIoPackageAccess')
    private void checkIfFileExists(String fileName) {
        File propsFile = new File(fileName) //NOPMD
        if ( !propsFile.exists() ) {
            out.println("Creating " + fileName)
            propsFile.createNewFile()
        }
    }

    private void addBuildSteps() {
        job.with {
            steps {
                downstreamParameterized {
                    trigger(projectName + '_washingmachine' + suffix) {
                        parameters {
                            propertiesFile(fileName)
                        }
                    }
                }
            }
        }
    }
}
