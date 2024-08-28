package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractConfigurableJobBuilder
import javaposse.jobdsl.dsl.Job

class CilWashingMachineEftfJobBuilder extends AbstractConfigurableJobBuilder {

    public Job build() {
        init('cil_washingmachine_eftf', 'eftf')

        configureProject()
                .setProjectDescriptionFromConfig()
                .discardOldBuilds(30, 30)
                .addParametersFromConfig()
                .restrictWhereThisProjectCanBeRun()
                .blockBuildIfJobsAreRunning('cil_washingmachine', 'GLOBAL', 'DISABLED')

        configureScm()
                .addGitRepository(config.gitRepoUrl, '*/master', true)

        configureBuildTriggers()
                .buildPeriodicallyFromConfig()

        configureBuildEnvironment()
                .deleteWorkspaceBeforeBuildStarts()
                .setJenkinsUserBuildVariables()

        configureBuildSteps()
                .executeShellForJobName(jobName)

        configurePostBuildSteps()
                .emailNotificationFromConfig()

        return job
    }
}
