package com.ericsson.bss.job.cus.ui

import com.ericsson.bss.job.NpmDeployReleaseFromMasterJobBuilder

class CusUiNpmDeployReleaseFromMasterJobBuilder extends NpmDeployReleaseFromMasterJobBuilder {
    @ Override
    protected String deploy() {
        targetScript = dslFactory.readFileFromWorkspace("scripts/npm_deploy_release.sh")

        insertPrePublishScript("scripts/cus/ui/pre_publish_script.sh")

        return targetScript
    }
}
