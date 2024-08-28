package com.ericsson.bss.job.eta

import com.ericsson.bss.Project
import com.ericsson.bss.job.GerritSonarJobBuilder

import static com.ericsson.bss.util.scriptbuilders.SonarReviewPosterScriptBuilder.getScript

class JenkinsDSLGerritSonarJobBuilder extends GerritSonarJobBuilder {

    protected static final SONAR_ANALYSIS_LOG_OUTPUT = super.SONAR_ANALYSIS_LOG_OUTPUT.replaceAll(">> console_out.txt", "")
    private final sonarReportJson = ".sonar/sonar-report.json"
    private final sonarIssuesReportPath = ".sonar/issues-report/"

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
                shell(SONAR_ANALYSIS_LOG_OUTPUT)
                if (generateGUIconfig) {
                    shell(getCDTSonarTesReportWorkaround())
                }
            }
            configure { project ->
                project / builders / 'hudson.plugins.sonar.SonarRunnerBuilder' {
                    installationName('https://sonar.epk.ericsson.se/')
                    properties(getSonarProperties())
                    jdk('(Inherit From Job)')
                }
            }
            steps {
                String mavenProjectFolder = mavenProjectLocation && mavenProjectLocation.length() > POM_XML.length() ?
                        mavenProjectLocation[0..-POM_XML.length()] : ""
                shell(getSonarPosterCommand(mavenProjectFolder))
                if (mavenProjectFolder) {
                    shell("# Copy the issues-report " +
                            "\nmkdir -p target/sonar/issues-report ; cp -r " + mavenProjectFolder +
                            "/target/sonar/issues-report/* target/sonar/issues-report")
                }
                shell(copySitePreview("./" + sonarIssuesReportPath + "*", "sonar", projectName))
                shell(grepSonarIssues())
            }
        }
    }

    @Override
    protected String grepSonarIssues() {
        return "" +
                getShellCommentDescription("Fail the job if new sonar issues found.") +
                "if grep -m 1 -q '\\\"isNew\\\":true' " + sonarReportJson + " ; then\n" +
                "false\n" +
                "else\n" +
                "true\n" +
                "fi"
    }

    @Override
    protected void setPublishers() {
        job.with {
            publishers {
                archiveArtifacts(sonarIssuesReportPath + '**/*')
            }
        }
        superSetPublishers()
    }

    @Override
    protected String gerritFeedbackFail() {
        String cmd = "ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT} " + getNotifySetting() + "-m '\"Sonar report: " +
                getSitePreviewUrl("sonar", projectName) + "'\${GERRIT_PATCHSET_REVISION}'/" + sonarIssuesReportPath +
                "issues-report.html \"' " + addGerritLabel("\${CODE_REVIEW}") + " \${GERRIT_PATCHSET_REVISION}"
        cmd += addRetry(cmd)

        String codeReview = "CODE_REVIEW=0\n" +
                "if grep -m 1 -q '\\\"isNew\\\":true' " + sonarReportJson + " ; then\n" +
                "CODE_REVIEW=-1\n" +
                "elif grep -m 1 -q '\\\"isNew\\\":false' " + sonarReportJson + " ; then\n" +
                "CODE_REVIEW=1\n" +
                "fi\n"

        return "" +
                codeReview +
                addGerritUser(jenkinsCodeQualityUser) +
                "if [ -f \"" + sonarIssuesReportPath + "issues-report.html\" ]; then \n" +
                "  echo \"Sonar report: " + getSitePreviewUrl("sonar", projectName) +
                "'\${GERRIT_PATCHSET_REVISION}'/" + sonarIssuesReportPath + "issues-report.html \"\n" +
                "   " + cmd + "\n" +
                "fi"
    }

    @Override
    protected String gerritFeedbackSuccess(boolean verboseGerritFeedback) {
        String cmd = "" +
                addGerritUser(jenkinsCodeQualityUser) +
                "if [ -f \"" + sonarIssuesReportPath + "issues-report.html\" ]; then \n" +
                "   echo \"Sonar report: " + getSitePreviewUrl("sonar", projectName) +
                "'\${GERRIT_PATCHSET_REVISION}'/" + sonarIssuesReportPath + "issues-report.html \" "

        String reviewCmd = "  ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT} " + getNotifySetting()

        if (verboseGerritFeedback) {
            reviewCmd += "" +
                    " -m '\"No new issues found related to this commit.\n" +
                    "To view existing issues, deselect \\\"Only NEW issues\\\" in the report.\n" +
                    getSitePreviewUrl("sonar", projectName) +
                    "'\${GERRIT_PATCHSET_REVISION}'/" + sonarIssuesReportPath + "issues-report.html \" " +
                    "" + addGerritLabel("1") + " \${GERRIT_PATCHSET_REVISION}"
        } else {
            reviewCmd += "" +
                    "  " + addGerritLabel("0") + " \${GERRIT_PATCHSET_REVISION}"
        }
        reviewCmd += addRetry(reviewCmd) + "\nfi"

        return cmd + reviewCmd
    }

    @Override
    protected String getSonarPosterCommand(String mavenProjectFolder) {
        String cmd = getShellCommentDescription("Sonar review poster")

        if (isUsingLabelForReviews()) {
            cmd += getScript(mavenProjectFolder, sonarReportJson, gerritServer, Project.GERRIT_EPK_USER)
        } else {
            cmd += getScript(mavenProjectFolder, sonarReportJson)
        }
        return cmd.replace(">> console_out.txt", "")
    }

    private static getSonarProperties() {
        return 'sonar.projectKey=com.ericsson.eta.jenkins.dsl\n' +
                'sonar.projectName=ETA Jenkins dsl\n' +
                'sonar.projectVersion=0.1\n' +
                'sonar.sources=src/main/groovy,jobs\n' +
                'sonar.tests=src/test/groovy\n' +
                'sonar.language=grvy\n' +
                'sonar.issuesReport.console.enable=true\n' +
                'sonar.issuesReport.html.enable=true\n' +
                'sonar.analysis.mode=incremental\n' +
                'sonar.profile=ETA\n' +
                'sonar.core.codeCoveragePlugin=jacoco'
    }

}
