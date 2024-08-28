package com.ericsson.bss.job.charging

import com.ericsson.bss.job.GerritSonarJobBuilder

class ChargingGerritSonarJobBuilder extends GerritSonarJobBuilder {
    protected void addJobs() {
        job.with {
            steps {
                shell(gitInitiateRepository("\${WORKSPACE}"))
                shell(gitConfig("\${WORKSPACE}"))
                shell(getGitCache("\${WORKSPACE}"))
                shell(gitFetchGerritChange())
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
                groovyCommand(verifyCommitMessageScript(), GROOVY_INSTALLATION_NAME)
                shell(gerritReportCommitMessage())
                shell(removeOldArtifacts())
                if (generateGUIconfig) {
                    shell(gconfWorkspaceWorkaround())
                }
                shell(getCoverageCommand())
                shell(SONAR_ANALYSIS_LOG_OUTPUT)
                if (generateGUIconfig) {
                    shell(getCDTSonarTesReportWorkaround())
                }
                shell(getMavenSonarCommand())
                String mavenProjectFolder = mavenProjectLocation && mavenProjectLocation.length() > POM_XML.length() ?
                        mavenProjectLocation[0..-POM_XML.length()]:""
                shell(getSonarPosterCommand(mavenProjectFolder))
                if (mavenProjectFolder) {
                    shell("# Copy the issues-report " +
                            "\nmkdir -p target/sonar/issues-report ; cp -r " + mavenProjectFolder +
                            "/target/sonar/issues-report/* target/sonar/issues-report")
                }
                shell(grepCommitMessageIssues())
                shell(copySitePreview("./target/sonar/issues-report/*", "sonar", projectName))
                shell(grepSonarIssues())
            }
        }
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
}
