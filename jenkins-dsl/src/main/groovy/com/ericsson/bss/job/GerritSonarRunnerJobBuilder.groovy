package com.ericsson.bss.job

import com.ericsson.bss.AbstractJobBuilder

class GerritSonarRunnerJobBuilder extends GerritSonarJobBuilder {

    protected static String sonarRunnerProperties = "sonar.host.url=https://sonar.epk.ericsson.se/\n" +
            "sonar.sourceEncoding=UTF-8\n" +
            "sonar.issuesReport.console.enable=true\n" +
            "sonar.issuesReport.html.enable=true\n" +
            "sonar.analysis.mode=incremental\n"

    @Override
    protected void addGerritSonarConfig() {
        super.addGerritSonarConfig()
        getSonarCommand()
    }

    @Override
    protected void addJobs() {
        job.with {
            steps {
                shell(gitInitiateRepository("\${WORKSPACE}"))
                shell(gitConfig("\${WORKSPACE}"))
                shell(getGitCache("\${WORKSPACE}"))
                shell(gitFetchGerritChange())
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
                groovyCommand(verifyCommitMessageScript(), AbstractJobBuilder.GROOVY_INSTALLATION_NAME)
                shell(gerritReportCommitMessage())
                shell(SONAR_ANALYSIS_LOG_OUTPUT)
                String coverageCommand = getCoverageCommand()
                if (coverageCommand != null) {
                    shell(coverageCommand)
                }
            }
        }
    }

    protected String getCoverageCommand() {
        return null
    }

    protected void getSonarCommand() {
        job.with {
            configure { project ->
                project / builders / 'hudson.plugins.sonar.SonarRunnerBuilder' / 'properties' <<
                        sonarRunnerProperties
            }

            String mavenProjectFolder = ""
            if (mavenProjectLocation) {
                mavenProjectFolder = mavenProjectLocation[0..-9] + "/"
            }

            configure { project ->
                project / builders / 'hudson.plugins.sonar.SonarRunnerBuilder' / 'project' <<
                        mavenProjectFolder + "sonar-runner.properties"
            }

            steps {
                shell("# Copy the issues-report " +
                        "\nmkdir -p target/sonar/issues-report ; cp -r .sonar/issues-report/*" +
                        " target/sonar/issues-report")
                shell(grepSonarIssues())
            }
        }
    }

    @Override
    protected String grepSonarIssues() {
        return getShellCommentDescription("Fail the job if new sonar issues found.") +
                "cat target/sonar/issues-report/issues-report-light.html | tr '\\n' ' ' " +
                "| grep '<h3>New issues</h3>         <span class=\"big\">0</span>'"
    }
}
