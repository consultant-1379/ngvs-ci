package com.ericsson.bss.job.chargingaccess

import com.ericsson.bss.job.MvnGerritUnitTestJobBuilder

class ChargingAccessGerritUnitTestJobBuilder extends MvnGerritUnitTestJobBuilder {
    @Override
    protected String mavenBuildCommand() {
        String cmd = "" +
                getShellCommentDescription("Maven build command") +
                "mvn \\\n" +
                "clean install \\\n"

        if (mavenProjectLocation){
            cmd += " -f " + mavenProjectLocation + " \\\n"
        }

        if (profilesToBeUsed != null && !profilesToBeUsed.equals("")) {
            cmd += '-P' + profilesToBeUsed + ' \\\n'
        }

        if (runParallelThreads && gerritName.equalsIgnoreCase("charging/com.ericsson.bss.rm.charging.access.integrationtest")) {
            cmd +=  "-DtestForkCount=7 \\\n"
        }
        else if (runParallelThreads) {
            cmd +=  "-DparallelTests -T 4 \\\n"
        }
        cmd += "-Dsurefire.useFile=false \\\n" +
                getMavenGeneralBuildParameters()

        return cmd
    }
}
