package com.ericsson.bss.job.cus.ui.assets

import com.ericsson.bss.job.NpmDeployReleaseFromPatchBranchJobBuilder

class CusUiAssetsNpmDeployReleaseFromPatchBranchJobBuilder extends NpmDeployReleaseFromPatchBranchJobBuilder {
    @ Override
    protected String deploy() {
        targetScript = dslFactory.readFileFromWorkspace("scripts/npm_deploy_release.sh")

        insertPrePublishScript("scripts/cus/ui/assets/pre_publish_script.sh")
        insertPostPublishScript("scripts/cus/ui/assets/post_publish_script.sh")

        return targetScript
    }
}
