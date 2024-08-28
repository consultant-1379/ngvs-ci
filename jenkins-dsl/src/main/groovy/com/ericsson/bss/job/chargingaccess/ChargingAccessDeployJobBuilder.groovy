package com.ericsson.bss.job.chargingaccess

import com.ericsson.bss.job.DeployJobBuilder

class ChargingAccessDeployJobBuilder extends DeployJobBuilder {

    @Override
    protected getDeployConfig() {
        workspacePath

        if (gerritName.equalsIgnoreCase("charging/com.ericsson.bss.rm.charging.access.integrationtest")) {
            extraMavenParameters = '-DtestForkCount=7 -DpublishToJivePortal=true'
        }

        def preSteps = {
            shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
            shell(removeOldArtifacts())
            shell(gitConfig("\${WORKSPACE}"))
            if (generateGUIconfig) {
                shell(gconfWorkspaceWorkaround())
            }
        }

        job.with {
            preBuildSteps(preSteps)

            Map environmentVariables = getInjectVariables()
            //We may need to change maven version to be used, depending if its master or release branch
            environmentVariables.put('M2_HOME', getMavenHome(branch))
            injectEnv(environmentVariables)

            addMavenConfig()
            if (workspacePath != null && !workspacePath.equals("")) {
                customWorkspace(workspacePath)
            }
            addMavenRelease()
            addTimeoutConfig()
        }
    }
}
