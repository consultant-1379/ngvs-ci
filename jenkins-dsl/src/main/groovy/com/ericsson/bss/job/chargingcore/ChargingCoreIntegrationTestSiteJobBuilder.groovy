package com.ericsson.bss.job.chargingcore

import com.ericsson.bss.job.SiteJobBuilder

public class ChargingCoreIntegrationTestSiteJobBuilder extends SiteJobBuilder {

    // Remove -DskipTest for integrationtest runtimeflow
    @Override
    protected String deploySiteCommand(String mavenSiteComm) {
        String cmd = getShellCommentDescription("Deploy maven site version and latest") +
                "mvn \\\n" +
                mavenSiteComm + " \\\n"

        if (profilesToBeUsed != null && profilesToBeUsed != "") {
            cmd += '-P' + profilesToBeUsed + ' \\\n'
            cmd += '-Dskip.cdt2.build=true \\\n'
        }
        cmd += getMavenGeneralBuildParameters()

        return cmd
    }
}
