package com.ericsson.bss.job.cel

import com.ericsson.bss.job.DeployJobBuilder

class CelDeployJobBuilder extends DeployJobBuilder {
    protected getDeployConfig() {
        Closure preSteps = {
            shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
            shell(removeOldArtifacts())
            shell(gitConfig("\${WORKSPACE}"))
            if (generateGUIconfig) {
                shell(gconfWorkspaceWorkaround())
            }
        }

        Map environmentVariables = getInjectVariables()

        job.with {
            preBuildSteps(preSteps)

            injectEnv(environmentVariables)

            addMavenConfig()
            addMavenRelease()
            addTimeoutConfig()
        }
    }

    protected void addReleaseConfig(String branchName) {
        job.with {
            description(JOB_DESCRIPTION)
            addGitRepository(gerritName, branchName)
            triggers {
                snapshotDependencies(true)
                scm(SCM_POLLING + '\n# Realtime pushed by the eta_gitscmpoll_trigger job')
            }
        }
    }
}

