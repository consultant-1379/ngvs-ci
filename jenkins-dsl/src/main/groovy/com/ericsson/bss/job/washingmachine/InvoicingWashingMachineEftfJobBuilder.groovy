package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractConfigurableJobBuilder
import javaposse.jobdsl.dsl.Job

class InvoicingWashingMachineEftfJobBuilder extends AbstractConfigurableJobBuilder {

    public Job build() {
        init('invoicing_washingmachine_eftf', 'eftf')

        configureProject()
                .setProjectDescriptionFromConfig()
                .discardOldBuilds(20, 100)
                .addParametersFromConfig()
                .restrictWhereThisProjectCanBeRun()

        configureBuildTriggers()

        configureBuildEnvironment()
                .deleteWorkspaceBeforeBuildStarts()
                .abortTheBuildIfItStuck(240)
                .addTimestampsToTheConsoleOutput()
                .setJenkinsUserBuildVariables()

        configureBuildSteps()
                .triggerCallBuildsOnOtherProjectsWithCurrentBuildParameters('invoicing_targethost_install', true, 'UNSTABLE', 'UNSTABLE', 'never')
                .executeShell("# Make sure all machines are up and running\nsleep 120")
                .executeShellForJobName(jobName)

        configurePostBuildSteps()
                .setBuildDescription()
                .emailNotificationFromConfig()

        return job
    }
}
