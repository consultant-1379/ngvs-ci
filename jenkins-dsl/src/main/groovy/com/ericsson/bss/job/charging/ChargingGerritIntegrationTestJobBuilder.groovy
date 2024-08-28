package com.ericsson.bss.job.charging

import com.ericsson.bss.job.GerritIntegrationTestJobBuilder

class ChargingGerritIntegrationTestJobBuilder extends GerritIntegrationTestJobBuilder {

    @Override
    protected void addIntegrationTestConfig() {
        job.with {
            String jobDescription = "<h2>Runs Integration test for repository.</h2>" +
                    "<p>This job will run all integration test against the commit in gerrit.</p>"

            description(DSL_DESCRIPTION + jobDescription)
            concurrentBuild()

            addChargingCoreIntegrationTests()
            addChargingAccessIntegrationTests()

            injectEnv(getInjectVariables())
            addTimeoutConfig()
        }
    }

    @Override
    protected String getTestsDirectory() {
        if (integrationTestRepository.contains('.access.')) {
            return '.integrationtest_access'
        } else {
            return ".integrationtest_core"
        }
    }

    @Override
    protected String mavenCommandToBuildIntegrationTestRepository(String testDirectory) {
        if (integrationTestRepository.contains('.access.')) {
            return getShellCommentDescription("Maven command to build and run Integration Test repository") +
                    "mvn \\\n" +
                    "-f " + testDirectory + "/pom.xml \\\n" +
                    "clean install \\\n" +
                    "-DtestForkCount=7 \\\n" +
                    "-Dsurefire.useFile=false \\\n" +
                    "-DfailIfNoTests=false \\\n" +
                    getMavenGeneralBuildParameters()
        } else {
            return super.mavenCommandToBuildIntegrationTestRepository(testDirectory)
        }
    }

    private void addChargingAccessIntegrationTests() {
        integrationTestRepository = gerritUser + "@" + gerritServer + ":29418/charging/com.ericsson.bss.rm.charging.access.integrationtest"
        addBuildSteps()
    }

    private void addChargingCoreIntegrationTests() {
        integrationTestRepository = gerritUser + "@" + gerritServer + ":29418/charging/com.ericsson.bss.rm.charging.integrationtest"
        addBuildSteps()
    }
}
