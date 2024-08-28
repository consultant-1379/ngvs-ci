package com.ericsson.bss.job

import com.ericsson.bss.AbstractJobBuilder

class PythonGerritSonarRunnerJobBuilder extends GerritSonarJobBuilder {

    private static String sonarRunnerProperties = "sonar.host.url=https://sonar.epk.ericsson.se/\n" +
            "sonar.sourceEncoding=UTF-8\n" +
            "sonar.issuesReport.console.enable=true\n" +
            "sonar.issuesReport.html.enable=true\n" +
            "sonar.analysis.mode=incremental\n"

    public String coverageTestFilePath = "tests/run_tests.py"
    public String coverageOmitPaths = "env/*,*tests/*"

    @Override
    protected void addGerritSonarConfig() {
        job.with {
            description(DSL_DESCRIPTION + JOB_DESCRIPTION)
            concurrentBuild()
            injectEnv(getInjectVariables())
            // TODO: Remove when jive supports special paths
            customWorkspace(CUSTOM_WORKSPACE_MESOS)
            addTimeoutConfig()
        }
        addJobs()
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
                shell(removeOldArtifacts())
                shell(SONAR_ANALYSIS_LOG_OUTPUT)
                shell(createVirtualEnvDir())
                shell(getCoverageCommand())
                shell(cleanVirtualEnvDir())
            }

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
                shell(copySitePreview("./target/sonar/issues-report/*", "sonar", projectName))
                shell(grepSonarIssues())
            }
        }
    }

    protected String getCoverageCommand() {
        String mavenSubProjectCmd = ""
        if (mavenProjectLocation) {
            mavenSubProjectCmd = mavenProjectLocation[0..-9] + "/"
        }
        // TODO: Make this less pythonic
        String cmd = ""
        cmd += "##############\n" +
                "#  Coverage  #\n" +
                "##############\n"
        cmd += "# Install packages and coverage\n" +
                "export PATH=/opt/local/dev_tools/python/2.7.10/bin/:\$PATH\n" +
                "export PATH=/proj/eta-tools/mongodb/2.4.9/Linux_x86_64/mongodb-linux-x86_64-2.4" +
                ".9/bin/:\$PATH\n" +
                "export PYTHONPATH=\${WORKSPACE}/" + mavenSubProjectCmd + "\n\n" +
                "cd \$VENV_TMP\n" +
                "virtualenv --system-site-packages env\n" +
                "source env/bin/activate\n" +
                "cd \${WORKSPACE}\n" +
                "pip install -r " + mavenSubProjectCmd + "pip_requirements.txt\n" +
                "pip install coverage\n\n" +
                "# Run the tests \n" +
                "python -m coverage run " + mavenSubProjectCmd + coverageTestFilePath + "  " +
                "--omit=" +
                coverageOmitPaths + "\n" +
                "python -m coverage html --omit=" + coverageOmitPaths + "\n" +
                "python -m coverage xml -i --omit=" + coverageOmitPaths
        return cmd
    }

    @Override
    protected String grepSonarIssues() {
        return getShellCommentDescription("Fail the job if new sonar issues found.") +
                "cat target/sonar/issues-report/issues-report-light.html | tr '\\n' ' ' " +
                "| grep '<h3>New issues</h3>         <span class=\"big\">0</span>'"
    }
}
