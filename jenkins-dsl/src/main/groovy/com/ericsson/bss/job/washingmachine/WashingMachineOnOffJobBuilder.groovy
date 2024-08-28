package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractConfigurableJobBuilder
import com.ericsson.bss.job.washingmachine.utils.WashingMachineOnOffScriptsBuilder
import javaposse.jobdsl.dsl.Job

class WashingMachineOnOffJobBuilder extends AbstractConfigurableJobBuilder {

    private String projectToBuildName

    /**
     * For normal washingmachines ex.: charging_washingmachine no suffix is needed. For rpm
     * washingmachines '_rpm' suffix is needed.
     * @param suffix
     * @return
     */
    public Job build(String suffix = '') {
        projectToBuildName = projectName + '_washingmachine' + suffix
        init(projectToBuildName + '_onoff', 'onoff')
        setRestrictLabel()

        configureProject()
                .setProjectDescriptionFromConfig()
                .addParametersFromConfig()

        configureBuildEnvironment()
                .addTimestampsToTheConsoleOutput()
                .setJenkinsUserBuildVariables()

        configureBuildSteps()
                .executeSystemGroovyScript(WashingMachineOnOffScriptsBuilder.newBuilder(dslFactory, projectToBuildName).build())

        configurePostBuildSteps()
                .setBuildDescription('^.*With description: (.*)')
                .editableEmailNotificationFromConfig()

        return job
    }
}
