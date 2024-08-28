package com.ericsson.bss.job

import com.ericsson.bss.util.Npm

class NpmPerformCodeAnalysisJobBuilder extends SonarRunnerJobBuilder {

    @Override
    protected void initShellJobs() {
        super.initShellJobs()
        shells.add(Npm.SHELL_TO_GENERATE_SONAR_PROP_FILE)
    }

    @Override
    protected void addGitRepository(String gerritName) {
        addGitRepository(gerritName, branchName)
    }
}
