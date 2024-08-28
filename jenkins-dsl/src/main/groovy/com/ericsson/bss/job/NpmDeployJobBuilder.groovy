package com.ericsson.bss.job

import javaposse.jobdsl.dsl.Job

public class NpmDeployJobBuilder extends DeployJobBuilder {

    protected String projectName = ""
    protected String branchName = ""
    protected String blameMailList = ""
    protected String npmRegistry = ""
    protected String testJobName = ""
    protected String codeAnalysisJobName = ""
    protected String releaseTag = ""
    protected String snapshotTag = ""
    protected String targetScript = ""

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addDeployConfig()

        return job
    }

    @Override
    protected void addDeployConfig() {
        // has to be overridden in derived classes
        super.addDeployConfig()
    }

    protected addPreBuildSteps() {
        job.with {
            steps {
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
                shell(gitConfig("\${WORKSPACE}"))
            }
        }
    }

    @Override
    protected getInjectVariables() {
        Map envList = super.getInjectVariables()

        envList.remove("PATH")
        envList.put("NPM_MODULES", "\${WORKSPACE}/node_modules/.bin")
        envList.put("NODE_PATH", "/opt/local/dev_tools/nodejs/node-v6.9.1-linux-x64/bin/")
        envList.put("NPM_CONFIG_USERCONFIG", "/proj/eta-automation/config/kascmadm/.npmrc")
        envList.put("DEVTOOLSET_2", "/opt/rh/devtoolset-2/root/usr/bin")
        envList.put("PATH", "\${DEVTOOLSET_2}:\${PYTHONPATH}/bin:\${GIT_HOME}/bin:\${M2}:\${NODE_PATH}:\${NPM_MODULES}:\${PATH}")
        envList.put("NPM_REGISTRY", npmRegistry)
        envList.put("BRANCH_NAME", branchName)
        envList.put("SNAPSHOT_TAG", snapshotTag)

        return envList
    }

    @Override
    protected getDeployConfig() {
        job.with {
            injectEnv(getInjectVariables())

            if (null != workspacePath && "" == workspacePath) {
                customWorkspace(workspacePath)
            }

            addNpmDeploy()
            addTimeoutConfig()
        }
    }

    protected addNpmDeploy() {
        // has to be overridden in derived classes
    }

    protected String readAndVerifyVersion() {
        return dslFactory.readFileFromWorkspace("scripts/npm_verify_package_version.groovy")
    }

    protected String verifyNpmRegistry() {
        return dslFactory.readFileFromWorkspace("scripts/npm_verify_registry_address.groovy")
    }

    protected String setNpmRegistry() {
        return getShellCommentDescription("Local npm registry") +
                "echo \"registry=\$NPM_REGISTRY\" >> \${WORKSPACE}/.npmrc"
    }

    protected String installNpmSnapshotModule() {
        return getShellCommentDescription("Install npm-snapshot module") +
                "npm install npm-snapshot"
    }

    protected String installNpmVersionModule() {
        return getShellCommentDescription("Install npm-version module") +
                "npm install npm-version"
    }

    protected String deploy() {
        // has to be overridden in derived classes
        return null
    }

    protected void insertPrePublishScript(String scriptsPath) {
        targetScript = targetScript.replaceAll(/# placeholder for pre-publish script.*/,
                                               dslFactory.readFileFromWorkspace(scriptsPath) )
    }

    protected void insertPostPublishScript(String scriptsPath) {
        targetScript = targetScript.replaceAll(/# placeholder for post-publish script.*/,
                                               dslFactory.readFileFromWorkspace(scriptsPath) )
    }
}
