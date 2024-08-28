package com.ericsson.bss.job.jive

import com.ericsson.bss.job.GerritSonarJobBuilder

class JiveGerritSonarJobBuilder extends GerritSonarJobBuilder {

    @Override
    protected void addGerritSonarConfig() {
        job.with {
            String jobDescription = "" +
                    "<h2>Run an incremental sonar analyze.</h2>" +
                    "<p>This jobs will run an analyze against sonar. The result is not saved.</p>"
            description(DSL_DESCRIPTION + jobDescription)
            concurrentBuild()
            injectEnv(getInjectVariables())
            // TODO: Remove when jive supports special paths
            customWorkspace(CUSTOM_WORKSPACE_MESOS)
            addTimeoutConfig()
        }
        addJobs()
    }

    @Override
    protected String gerritFeedbackFail() {
        String gerritFeedbackFail = dslFactory.readFileFromWorkspace('scripts/gerrit_sonar_fail_feedback.sh')
        gerritFeedbackFail = gerritFeedbackFail.replace('GERRIT_USER="%CODE_QUALITY_USER%"',
                addGerritUser(jenkinsCodeQualityUser)).replaceAll('%GERRIT_SERVER%', gerritServer)

        String sitePreview = getSitePreviewUrl("sonar", projectName) + "'\${GERRIT_PATCHSET_REVISION}'/target/sonar/issues-report/issues-report" +
                ".html"
        gerritFeedbackFail = gerritFeedbackFail.replace("%SITE_PREVIEW%", sitePreview)

        if (isUsingLabelForReviews()) {
            gerritFeedbackFail = gerritFeedbackFail
                    .replace("-l Code-Review=", "--label Code-Quality=")
        }

        return gerritFeedbackFail
    }

    @Override
    protected String getCoverageCommand() {
        String mavenSubProjectCmd = ""
        if (mavenProjectLocation) {
            mavenSubProjectCmd = " -f " + mavenProjectLocation
        }
        String cmd = getShellCommentDescription("Command to generate coverage report") +
                "mvn " + mavenSubProjectCmd +  " \\\n"

        cmd += "clean install \\\n" +
                "-Dcobertura.skip=true \\\n"

        cmd += "-Dsurefire.useFile=false \\\n" +
                getMavenGeneralBuildParameters() + " \\\n" +
                "-Dmaven.test.failure.ignore=true"

        return cmd
    }
}
