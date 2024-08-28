package com.ericsson.bss.job.chargingcore

import com.ericsson.bss.job.GerritSiteJobBuilder

public class ChargingCoreIntegrationTestGerritSiteJobBuilder extends GerritSiteJobBuilder {

    // Remove -DskipTest for integrationtest runtimeflow
    @Override
    protected String mavenBuildCommand() {
        super.mavenBuildCommand().replace('-DskipTests ', '')
    }
}
