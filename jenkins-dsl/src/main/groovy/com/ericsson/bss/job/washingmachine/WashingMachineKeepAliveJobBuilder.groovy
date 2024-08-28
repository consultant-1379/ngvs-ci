package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractConfigurableJobBuilder
import com.ericsson.bss.job.washingmachine.utils.WashingMachineOnOffScriptsBuilder
import javaposse.jobdsl.dsl.Job

class WashingMachineKeepAliveJobBuilder extends AbstractConfigurableJobBuilder {

    private String projectToBuildName

    public Job build(String suffix = '') {
        projectToBuildName = projectName + '_washingmachine' + suffix
        init(projectToBuildName + '_keepalive', 'keepalive')
        Map properties = readConfig()

        configureProject()
                .discardOldBuilds(20, 20)
                .blockBuildIfCertainJobsAreRunning(getJobsToWaitFor())
                .restrictWhereThisProjectCanBeRun()
                .quietPeriod(20)

        setRestrictLabel(RESTRICT_LABEL_MESOS_LIGHT)

        String schema = 'H/30 * * * *'
        if (suffix.contains("rpm")) {
            if (projectName.equals("rmca")) {
                schema = 'H/15 4-23,0-1 * * *'
            }
            else {
                schema = 'H/10 4-23,0-1 * * *'
            }
        }
        configureBuildTriggers().buildPeriodically(schema)

        configureBuildEnvironment()
                .addTimestampsToTheConsoleOutput()

        if (WashingMachineOnOffScriptsBuilder.ifProjectNeedParamsFromFile(projectToBuildName)) {
            configureBuildSteps()
                    .triggerCallBuildsOnOtherProjectsWithParametersFromFile(projectToBuildName,
                    WashingMachineOnOffScriptsBuilder.getProjectParamsFile(projectToBuildName))
        } else {
            configureBuildSteps()
                    .triggerCallBuildsOnOtherProjectsWithCurrentBuildParameters(projectToBuildName)
        }

        if (properties['JOB_ENABLED'] != null && !Boolean.parseBoolean(properties['JOB_ENABLED'])) {
            disable()
        } else {
            enable()
        }

        return job
    }

    private String getJobsToWaitFor() {
        String jobs = projectToBuildName
        if (config.additionalJobsToWaitWithBuild) {
            jobs += ',' + config.additionalJobsToWaitWithBuild
        }
        return jobs
    }

    private Map readConfig() {
        Properties properties = new Properties()
        String confFile = WashingMachineOnOffScriptsBuilder.getProjectParamsFile(projectToBuildName)
        FileReader fr = new FileReader(confFile)
        properties.load(fr)
        return properties
    }
}
