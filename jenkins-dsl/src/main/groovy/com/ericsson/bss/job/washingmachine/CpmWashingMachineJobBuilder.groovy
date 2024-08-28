package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractConfigurableJobBuilder
import javaposse.jobdsl.dsl.Job

class CpmWashingMachineJobBuilder extends AbstractConfigurableJobBuilder {

    protected static final int DEFAULT_TIMEOUT = 240
    protected static final int DEFAULT_TAPAS_TIMEOUT = (DEFAULT_TIMEOUT-10)*60

    public Job build() {
        init('cpm_washingmachine', 'normal')

        configureProject()
                .setProjectDescriptionFromConfig()
                .discardOldBuilds(30, 45)
                .restrictWhereThisProjectCanBeRun()
                .permissionToCopyArtifact(config.projectsToTrigger.projects.join(','))

        configureBuildEnvironment()
                .failTheBuildIfItStuck(240)
                .setJenkinsUserBuildVariables()
                .addTimestampsToTheConsoleOutput()

        configureBuildSteps()
                .executeShell(dslFactory.readFileFromWorkspace('scripts/washingmachine/cpm_washingmachine_pre_tapas.sh'))
                .executeShell(dslFactory.readFileFromWorkspace('scripts/washingmachine/' + jobName + '.sh')
                    .replaceAll("killtimeout", DEFAULT_TAPAS_TIMEOUT.toString()))
                .executeShell(dslFactory.readFileFromWorkspace('scripts/washingmachine/cpm_washingmachine_post_tapas.sh'))

        configurePostBuildSteps()
                .archiveTheArtifacts(["cpmrpms/*.rpm", "*/jive/*.jar"], true, false, false, true)
                .jenkinsTextFinder('^.*Reporting end of session with status 2.*$', '', true, false, true)
                .publishJUnitTestResultReport('**/surefire-reports/*.xml')
                .setBuildDescription()
                .executeGroovyScript(dslFactory.readFileFromWorkspace('scripts/washingmachine/wm_blame_status.groovy'))
                .executeASetOfScriptsFromConfig()

        return job
    }
}
