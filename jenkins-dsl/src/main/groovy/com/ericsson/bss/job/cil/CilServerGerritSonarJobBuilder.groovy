package com.ericsson.bss.job.cil

import com.ericsson.bss.job.GerritSonarJobBuilder

class CilServerGerritSonarJobBuilder extends GerritSonarJobBuilder{
    protected void addJobs() {
        job.with {
            steps {
                shell(symlinkMesosWorkSpace())
                shell(gitInitiateRepository("\${WORKSPACE}"))
                shell(gitConfig("\${WORKSPACE}"))
                shell(getGitCache("\${WORKSPACE}"))
                shell(gitFetchGerritChange())
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
                groovyCommand(verifyCommitMessageScript(), GROOVY_INSTALLATION_NAME)
                shell(gerritReportCommitMessage())
                shell(setCilServerVirtualenvDir())
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
                shell(copySitePreview("./target/sonar/issues-report/*", "sonar", projectName))
                shell(grepSonarIssues())
            }
        }
    }
}
