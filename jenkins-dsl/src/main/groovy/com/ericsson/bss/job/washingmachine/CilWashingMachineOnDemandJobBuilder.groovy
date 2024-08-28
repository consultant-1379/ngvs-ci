package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractConfigurableJobBuilder
import javaposse.jobdsl.dsl.Job

class CilWashingMachineOnDemandJobBuilder extends AbstractConfigurableJobBuilder {

    public Job build() {
        init('cil_washingmachine_ondemand', 'ondemand')

        configureProject()
                .setProjectDescriptionFromConfig()
                .discardOldBuilds(30, 30)
                .addParametersFromConfig()
                .restrictWhereThisProjectCanBeRun()

        configureScm()
                .addGitRepository(config.gitRepoUrl, '*/master', true)

        configureBuildEnvironment()
                .deleteWorkspaceBeforeBuildStarts()
                .setJenkinsUserBuildVariables()
                .addTimestampsToTheConsoleOutput()

        configureBuildSteps()
                .executeShell(dslFactory.readFileFromWorkspace('scripts/washingmachine/' + jobName + '_1.sh'))
                .executeShell(dslFactory.readFileFromWorkspace('scripts/washingmachine/' + jobName + '_2.sh'))

        configurePostBuildSteps()
                .emailNotificationFromConfig()

        return job
    }
}
