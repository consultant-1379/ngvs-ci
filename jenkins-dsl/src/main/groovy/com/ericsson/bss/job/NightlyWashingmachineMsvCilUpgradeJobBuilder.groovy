package com.ericsson.bss.job

import com.ericsson.bss.AbstractTapasJobBuilder
import javaposse.jobdsl.dsl.Job

public class NightlyWashingmachineMsvCilUpgradeJobBuilder extends AbstractTapasJobBuilder {

    protected String jenkinsURL = ""
    protected String msvResourceProfile = "Washingmachine"
    protected String cilResourceProfile = "Washingmachine"
    protected int cronTrigger = 0

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        setDescription()
        setConcurrentBuild(false)
        buildPeriodically("H " + cronTrigger + " * * *")
        setEnvironmentVariables()
        discardOldBuilds(30, 30)
        deleteWorkspaceBeforeBuildStarts()
        addBuildSteps()
        deleteWorkspaceAfterBuild()

        return job
    }

    @Override
    protected void setDescription() {
        String setMsvCilVersion = jenkinsURL + "job/" + tpgName + "_set_msv_cil_version/"
        String addRemoveNightlyWM = jenkinsURL + "job/" + tpgName + "_add_remove_nightly_washingmachine_msv_cil_upgrade"

        job.with {
            description(DSL_DESCRIPTION +
                '<h2>This job upgrades the MSV and CIL versions of ' + projectName +
                '\'s Washingmachine clusters</h2>' +
                'The job will run once every night and go through all clusters ' +
                'specificed by <a href ="'+ addRemoveNightlyWM + '" />' + tpgName +
                '_add_remove_nightly_washingmachine_msv_cil_upgrade</a>.' +
                '<br>The MSV and CIL versions used in the upgrades is specificed by ' +
                '<a href ="'+ setMsvCilVersion + '" />' + tpgName + '_set_msv_cil_version</a>.')
        }
    }

    @Override
    protected void setTapasShell() {
    }

    @Override
    protected void archiveArtifacts() {
    }

    private void setEnvironmentVariables() {
        Map variables = [:]
        variables.put("#Set true to force this job to reinstall the MSV and/or CIL", "")
        variables.put("#on all clusters currently in the list", "")
        variables.put("FORCE_MSV_CIL_INSTALLATION", "false")

        injectPortAllocation = ""
        injectEnv(variables)
    }

    protected void addBuildSteps() {
        job.with {
            steps {
                systemGroovyCommand(getGroovyScript("scripts/nightly_washingmachine_msv_cil_" +
                                                    "upgrade.groovy"))
            }
        }
    }

    private String getGroovyScript(String path) {
        String script = dslFactory.readFileFromWorkspace(path)
        return script.replaceAll("\\{TPG\\}", projectName)
                     .replaceAll("\\{MSV_RESOURCE_PROFILE\\}", msvResourceProfile)
                     .replaceAll("\\{CIL_RESOURCE_PROFILE\\}", cilResourceProfile)
    }
}
