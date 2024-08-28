package com.ericsson.bss.job.cus.ui

import com.ericsson.bss.job.NpmDeploySnapshotJobBuilder

class CusUiNpmDeploySnapshotJobBuilder extends NpmDeploySnapshotJobBuilder {
    @ Override
    protected String deploy() {
        targetScript = dslFactory.readFileFromWorkspace("scripts/npm_deploy_snapshot.sh")

        insertPrePublishScript("scripts/cus/ui/pre_publish_script.sh")

        return targetScript
    }
}
