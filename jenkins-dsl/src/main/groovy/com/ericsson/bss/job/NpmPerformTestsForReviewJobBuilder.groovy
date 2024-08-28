package com.ericsson.bss.job

import javaposse.jobdsl.dsl.Job

public class NpmPerformTestsForReviewJobBuilder extends GerritUnitTestJobBuilder {

    protected String projectName
    protected Map envList
    protected String branchName
    protected String npmRegistry
    protected final static String JOB_DESCRIPTION = "This job is used to perform " +
            "tests of NPM-based projects. Normally this job is being triggered \n" +
            "automatically by Gerrit after creation of a new patch-set."

    @Override
    public Job build(params) {
        Job job = super.build(branchName: branchName)
        job.with {
            description(DSL_DESCRIPTION + JOB_DESCRIPTION + BSSF_MAVEN_CI_DESCRIPTION)}
        return job
    }

    @Override
    protected void setEnvVariables() {
        envList = getInjectVariables()
        envList.remove("PATH")
        envList.put("NPM_MODULES", "\${WORKSPACE}/node_modules/.bin")
        envList.put("NODE_PATH", "/opt/local/dev_tools/nodejs/node-v6.9.1-linux-x64/bin/")
        envList.put("NPM_CONFIG_USERCONFIG", "/proj/eta-automation/config/kascmadm/.npmrc")
        envList.put("DEVTOOLSET_2", "/opt/rh/devtoolset-2/root/usr/bin")
        envList.put("PATH", "\${DEVTOOLSET_2}:\${PYTHONPATH}/bin:\${GIT_HOME}/bin:\${M2}:\${NODE_PATH}:\${NPM_MODULES}:\${PATH}")
        envList.put("NPM_REGISTRY", npmRegistry)
        envList.put("BRANCH_NAME", branchName)
    }

    @Override
    protected void extraShellSteps() {
        job.with {
            steps {
                shell(setNpmRegistry())
                shell(installDependencies())
                shell(performTests())
            }
        }
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
