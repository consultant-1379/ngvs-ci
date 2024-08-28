package com.ericsson.bss.job.jive

import com.ericsson.bss.job.SonarJobBuilder

class JiveSonarJobBuilder extends SonarJobBuilder{

    @Override
    protected void addSonarConfig() {
        job.with {
            description(DSL_DESCRIPTION + JOB_DESCRIPTION)

            addGitRepository(gerritName, branchName)
            customWorkspace(CUSTOM_WORKSPACE_MESOS)
            addTimeoutConfig()

            steps {
                for (shellStep in shells) {
                    shell(shellStep)
                }

                if (jobName.contains("jive-core_sonar")) {
                    copyArtifacts('jive_integration_tests') {
                        includePatterns('**/jive-domain-examples/target/jacoco-it.exec')
                    }
                    copyArtifacts('jive_integration_tests') {
                        includePatterns('**/jive-protocols/target/jacoco-it.exec')
                    }
                    String copyArtifactsCommand = "# Copy coverage to all target folders\n" +
                            "find . -type d -name 'target' -exec cp --no-clobber " +
                            "\${WORKSPACE}/jive-examples/jive-domain-examples/target/jacoco-it.exec {} \\;\n" +
                            "cp \${WORKSPACE}/jive-common/jive-protocols/target/jacoco-it.exec jive-converters/target/\n" +
                            "cp \${WORKSPACE}/jive-common/jive-protocols/target/jacoco-it.exec jive-converters-maven-plugin/target/"
                    shell(copyArtifactsCommand)
                }
            }
            injectEnv(getInjectVariables())
            addSonarNature()
            addExtendableEmail()

            publishers { wsCleanup() }
        }
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
