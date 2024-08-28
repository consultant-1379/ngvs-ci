package com.ericsson.bss.job.chargingaccess

import com.ericsson.bss.job.GerritIntegrationTestJobBuilder

class ChargingAccessGerritIntegrationTestJobBuilder extends GerritIntegrationTestJobBuilder {

    @Override
    protected String mavenCommandToBuildIntegrationTestRepository(String testDirectory) {
        return "" +
                getShellCommentDescription("Maven command to build and run Integration Test repository") +
                "mvn \\\n" +
                "-f " + testDirectory + "/pom.xml \\\n" +
                "clean install \\\n" +
                "-DtestForkCount=7 \\\n" +
                "-Dsurefire.useFile=false \\\n" +
                "-DfailIfNoTests=false \\\n" +
                getMavenGeneralBuildParameters()
    }
}
