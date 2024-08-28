package com.ericsson.bss.job

import com.ericsson.bss.util.Npm

class NpmPerformCodeAnalysisForReviewJobBuilder extends GerritSonarRunnerJobBuilder {

    @Override
    protected void addJobs() {
        super.addJobs()
        job.with {
            steps {
                shell(Npm.SHELL_TO_GENERATE_SONAR_PROP_FILE)
            }
        }
    }
}
