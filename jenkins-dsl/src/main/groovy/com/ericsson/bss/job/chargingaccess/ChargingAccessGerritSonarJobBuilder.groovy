package com.ericsson.bss.job.chargingaccess

import com.ericsson.bss.job.GerritSonarJobBuilder

class ChargingAccessGerritSonarJobBuilder extends GerritSonarJobBuilder{
    protected void addGerritSonarConfig() {
        if (gerritName.equalsIgnoreCase("charging/com.ericsson.bss.rm.charging.access.plugins")){
            runParallelThreads = false
        }
        super.addGerritSonarConfig()
        job.with {
            Map<String, String> env_list = getInjectVariables()
            if (gerritName.equalsIgnoreCase("charging/com.ericsson.bss.rm.charging.access.plugins")) {
                String javaToolOptions = env_list.get('JAVA_TOOL_OPTIONS').replaceFirst('-XX:MaxPermSize.*? ', '-XX:MaxPermSize=1G ')
                env_list.put('JAVA_TOOL_OPTIONS', javaToolOptions)
                String mavenOpts = env_list.get('MAVEN_OPTS').replaceFirst('-XX:MaxPermSize.*? ', '-XX:MaxPermSize=1G ')
                env_list.put('MAVEN_OPTS', mavenOpts)
            }
            injectEnv(env_list)
        }
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
                shell(copySitePreview("./target/sonar/issues-report/*", "sonar", projectName))
                shell(grepSonarIssues())
            }
        }
    }

    @Override
    protected String getCoverageCommand() {
        if (gerritName.equalsIgnoreCase("charging/com.ericsson.bss.rm.charging.access.integrationtest")) {
            String mavenSubProjectCmd = ""
            if (mavenProjectLocation){
                mavenSubProjectCmd = " -f " + mavenProjectLocation
            }
            String cmd = "" +
                    getShellCommentDescription("Command to generate coverage report") +
                    "mvn " + mavenSubProjectCmd +  " \\\n" +
                    "clean "

            if (generateCoberturaReport) {
                cmd += 'cobertura:cobertura -Dcobertura.report.format=xml '
            }
            else {
                cmd += JACOCO_AGENT + ' '
            }

            cmd += "install \\\n"

            if (runParallelThreads || generateCoberturaReport) {
                cmd += "-DtestForkCount=7 \\\n"
            }
            cmd += "-Dsurefire.useFile=false \\\n" +
                    getMavenGeneralBuildParameters() + " \\\n" +
                    "> console_out.txt" + " || true"
            return cmd
        }
        else {
            super.getCoverageCommand()
        }
    }
}
