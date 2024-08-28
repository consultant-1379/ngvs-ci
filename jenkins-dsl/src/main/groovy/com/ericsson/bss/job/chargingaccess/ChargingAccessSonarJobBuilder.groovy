package com.ericsson.bss.job.chargingaccess

import com.ericsson.bss.job.SonarJobBuilder

class ChargingAccessSonarJobBuilder extends SonarJobBuilder {

    @Override
    protected String getCoverageCommand() {
        String mavenSubProjectCmd = ""
        if (mavenProjectLocation){
            mavenSubProjectCmd = " -f " + mavenProjectLocation
        }
        String cmd = getShellCommentDescription("Command to generate coverage report") +
                "mvn " + mavenSubProjectCmd +  " \\\n"

        if (generateCoberturaReport) {
            cmd += "" +
                    "clean cobertura:cobertura -Dcobertura.report.format=xml install \\\n"
        }
        else {
            cmd += "" +
                    "clean " + JACOCO_AGENT + " install \\\n" +
                    "-Dcobertura.skip=true \\\n"
        }

        if (profilesToBeUsed != null && !profilesToBeUsed.equals("")) {
            cmd += ' -P' + profilesToBeUsed + ' '
        }

        if (gerritName.equalsIgnoreCase("charging/com.ericsson.bss.rm.charging.access.integrationtest")) {
            cmd+= "-DtestForkCount=7 \\\n"
        }
        cmd += "-Dsurefire.useFile=false \\\n" +
                getMavenGeneralBuildParameters() + " \\\n" +
                "-Dmaven.test.failure.ignore=true"

        return cmd
    }
}
