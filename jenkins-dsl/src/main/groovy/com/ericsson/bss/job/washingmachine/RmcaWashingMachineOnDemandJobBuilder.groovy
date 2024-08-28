package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractConfigurableJobBuilder
import javaposse.jobdsl.dsl.Job

class RmcaWashingMachineOnDemandJobBuilder extends AbstractConfigurableJobBuilder {

    public Job build() {
        init('rmca_washingmachine_ondemand', 'ondemand')

        configureProject()
                .setProjectDescriptionFromConfig()
                .discardOldBuilds(20, 20)
                .restrictWhereThisProjectCanBeRun()
                .executeConcurrentBuildsIfNecessary()
                .addParametersFromConfig()

        configureBuildEnvironment()
                .deleteWorkspaceBeforeBuildStarts()
                .runXvfbDuringBuild()
                .abortTheBuildIfItStuck(240)
                .injectEnvironmentVariablesToTheBuildProcess(config.common_env_variables, dslFactory.readFileFromWorkspace(injectPortAllocation))
                .setJenkinsUserBuildVariables()
                .addTimestampsToTheConsoleOutput()

        configureBuildSteps()
                .executeShell(symlinkMesosWorkSpace())
                .executeShellForJobName(jobName)

        configurePostBuildSteps()
                .jenkinsTextFinder()
                .setBuildDescription()
                .executeASetOfScripts()
                .editableEmailNotificationFromConfig()

        return job
    }
}
