package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractConfigurableJobBuilder
import javaposse.jobdsl.dsl.Job

class ChargingWashingMachineOnDemandJobBuilder extends AbstractConfigurableJobBuilder {

    public Job build() {
        init('charging_washingmachine_ondemand', 'ondemand')

        configureProject()
                .setProjectDescriptionFromConfig()
                .discardOldBuilds(20, 20)
                .addParametersFromConfig()
                .restrictWhereThisProjectCanBeRun()
                .executeConcurrentBuildsIfNecessary()

        configureBuildEnvironment()
                .deleteWorkspaceBeforeBuildStarts()
                .abortTheBuildIfItStuck(240)
                .setJenkinsUserBuildVariables()
                .addTimestampsToTheConsoleOutput()

        configureBuildSteps()
                .executeShellForJobName(jobName)

        configurePostBuildSteps()
                .jenkinsTextFinder()
                .setBuildDescription()
                .editableEmailNotificationFromConfig()

        return job
    }
}
