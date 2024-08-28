package com.ericsson.bss.job.cus.ui.assets

import com.ericsson.bss.job.NpmDeploySnapshotJobBuilder

class CusUiAssetsNpmDeploySnapshotJobBuilder extends NpmDeploySnapshotJobBuilder {
    @ Override
    protected String deploy() {
        targetScript = dslFactory.readFileFromWorkspace("scripts/npm_deploy_snapshot.sh")

        insertPrePublishScript("scripts/cus/ui/assets/pre_publish_script.sh")

        return targetScript
    }
}
