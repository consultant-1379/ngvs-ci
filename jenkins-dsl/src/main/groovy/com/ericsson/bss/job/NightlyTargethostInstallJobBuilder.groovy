package com.ericsson.bss.job

import com.ericsson.bss.AbstractTapasJobBuilder
import javaposse.jobdsl.dsl.Job

public class NightlyTargethostInstallJobBuilder extends AbstractTapasJobBuilder {

    protected int cronTrigger = 0
    protected LinkedHashMap<String,String> targethostInstallParameters = [:]
    protected jenkinsURL = ""
    protected String msvResourceProfile = "TestSystem"
    protected String cilResourceProfile = "TestSystem"

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
        String targethostInstall = (jenkinsURL + "job/" + tpgName +
                                   "_add_remove_nightly_full_targethost_install")
        job.with {
            description(DSL_DESCRIPTION +
                       '<h2>This job will run ' + tpgName + '_targethost_install every night ' +
                       'based on list.</h2>You can add or remove your cluster to the list here: ' +
                       '<a href="' + targethostInstall + '">ADD/REMOVE</a>.')
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
                systemGroovyCommand(getGroovyScript("scripts/nightly_targethost_install_msv_cil_upgrade.groovy"))
            }
        }
    }

    private String getGroovyScript(String path) {

        String params = ""
        targethostInstallParameters.each { k, v ->
            params += "    params.add(new StringParameterValue('${k}', node.@${v}))\n"
        }
        params += "\n"

        String script = dslFactory.readFileFromWorkspace(path)
        return script.replaceAll("\\{TPG\\}", tpgName)
                     .replaceAll("\\{TPG_TARGETHOST_PARAMETERS\\}", params)
                     .replaceAll("\\{MSV_RESOURCE_PROFILE\\}", msvResourceProfile)
                     .replaceAll("\\{CIL_RESOURCE_PROFILE\\}", cilResourceProfile)
    }
}
