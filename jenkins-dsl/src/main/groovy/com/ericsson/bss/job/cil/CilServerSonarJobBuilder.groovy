package com.ericsson.bss.job.cil

import com.ericsson.bss.job.SonarJobBuilder

class CilServerSonarJobBuilder extends SonarJobBuilder{
    protected void initShellJobs(){
        shells.add(symlinkMesosWorkSpace())
        shells.add(cleanUpWorkspaceMesos("\${WORKSPACE}"))
        shells.add(removeOldArtifacts())
        shells.add(gitConfig("\${WORKSPACE}"))
        if (branchName != "master") {
            shells.add(getBranchRenameCommand())
        }
        shells.add(setCilServerVirtualenvDir())
        shells.add(getCoverageCommand())
        if (generateGUIconfig) {
            shells.add(getCDTSonarTesReportWorkaround())
        }
    }
}
