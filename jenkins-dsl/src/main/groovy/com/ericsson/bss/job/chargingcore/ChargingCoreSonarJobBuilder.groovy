package com.ericsson.bss.job.chargingcore

import com.ericsson.bss.job.SonarJobBuilder

public class ChargingCoreSonarJobBuilder extends SonarJobBuilder {

    @Override
    protected void initShellJobs() {
        super.initShellJobs()
        shells.add(getDownloadITCoverageReportCommand())
        shells.add(getCopyITCoverageReportToModulesCommand())
    }

    private String getDownloadITCoverageReportCommand() {
        return '' +
                getShellCommentDescription("Download IT coverage report") +
                'mvn dependency:copy -Dartifact=com.ericsson.bss.rm.charging.integrationtest:testcases:LATEST:exec:jacoco-it -DoutputDirectory=coverage \\\n' +
                getMavenGeneralBuildParameters()
    }

    private String getCopyITCoverageReportToModulesCommand() {
        return '' +
                getShellCommentDescription("Copy IT coverage report to modules") +
                'mv coverage/*-jacoco-it.exec coverage/jacoco-it.exec\n' +
                'JACOCO_IT_FILE=\${WORKSPACE}/coverage/jacoco-it.exec\n' +
                'find . -type d -name \'target\' -exec cp --no-clobber \${JACOCO_IT_FILE} {} \\;'
    }
}
