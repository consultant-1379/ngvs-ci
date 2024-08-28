package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractJobBuilder
import javaposse.jobdsl.dsl.Job

class CpmWashingMachineGraphsJobBuilder extends AbstractJobBuilder {

    public Job build() {
        init('cpm_washingmachine_graphs')

        configureProject()
                .setProjectDescription('Updates the graphs after successfull execution of the washingmnachine (rrdtool is not installed on the agents, ' +
                'therefore this must be run on <i>master</i>).')
                .discardOldBuilds(30, 20)
                .restrictWhereThisProjectCanBeRun('master')

        configureBuildTriggers()
                .buildAfterOtherProjectsAreBuilt('cpm_washingmachine', 'FAILURE')

        configureBuildEnvironment()
                .abortTheBuildIfItStuck(3)
                .addTimestampsToTheConsoleOutput()

        configureBuildSteps()
                .executeShellForJobName(jobName)

        return job
    }
}
