package com.ericsson.bss.job

import com.ericsson.bss.AbstractJobBuilder
import javaposse.jobdsl.dsl.Job

class NpmPerformTestsJobBuilder extends AbstractJobBuilder {

    protected Map envList
    protected String npmRegistry = ""
    protected String branchName = ""
    protected final static String JOB_DESCRIPTION = "This job is used to perform " +
            "tests of NPM-based projects. Normally this job is being triggered \n" +
            "manually by user or automatically by other jobs \n" +
            "(e.g. 'deploy_release' or 'deploy_snapshot')."

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addGitRepository(gerritName, branchName)
        injectEnv(getInjectVariables())
        addDescription()
        addPreBuildSteps()
        addBuildSteps()

        return job
    }

    protected void addDescription() {
        job.with {
            description(DSL_DESCRIPTION + JOB_DESCRIPTION)}
    }

    protected addPreBuildSteps() {
        job.with {
            steps {
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
                shell(gitConfig("\${WORKSPACE}"))
            }
        }
    }

    protected void addBuildSteps() {
        job.with {
            steps {
                shell(setNpmRegistry())
                shell(installDependencies())
                shell(performTests())
            }
        }
    }

    @Override
    protected getInjectVariables() {
        envList = super.getInjectVariables()

        envList.remove("PATH")
        envList.put("NPM_MODULES", "\${WORKSPACE}/node_modules/.bin")
        envList.put("NODE_PATH", "/opt/local/dev_tools/nodejs/node-v6.9.1-linux-x64/bin/")
        envList.put("NPM_CONFIG_USERCONFIG", "/proj/eta-automation/config/kascmadm/.npmrc")
        envList.put("DEVTOOLSET_2", "/opt/rh/devtoolset-2/root/usr/bin")
        envList.put("PATH", "\${DEVTOOLSET_2}:\${PYTHONPATH}/bin:\${GIT_HOME}/bin:\${M2}:\${NODE_PATH}:\${NPM_MODULES}:\${PATH}")
        envList.put("NPM_REGISTRY", npmRegistry)

        return envList
    }

    protected String setNpmRegistry() {
        return getShellCommentDescription("Local npm registry") +
                "echo \"registry=\$NPM_REGISTRY\" >> \${WORKSPACE}/.npmrc"
    }

    protected String installDependencies() {
        String cmd = getShellCommentDescription("Install dependent modules") +
                "npm install"

        return cmd
    }

    protected String performTests() {
        String cmd = getShellCommentDescription("Run npm test command") +
                "npm run test"

        return cmd
    }
}
