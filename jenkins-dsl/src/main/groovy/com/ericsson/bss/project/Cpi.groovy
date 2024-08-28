package com.ericsson.bss.project

import com.ericsson.bss.Project

class Cpi extends Project {
    public static String projectName = "cpi"

    public Cpi() {
        super.projectName = this.projectName
    }

    public void create(parent) {
        this.init(parent)
        super.createWashingMachinesJobs()
        super.createOvfBuildJob('CPI/Build%20CPI%20OVF', 'suites/build_ovf.xml',
                'build_ovf_\${TARGETHOST}.xml')

        super.createTargethostInstallJob(
                versionLocation: ['https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/CPI/,' +
                                 'https://arm.epk.ericsson.se/artifactory/proj-cpi-release/com/ericsson/bss/cpi/package/umi/cpi/;1.1.0'],
                defaultTapasJobPath: 'CPI/Targethost%20CPI%20Install',
                useDvFile: true,
                useJiveTests: true,
                jiveMetaData: "https://arm.epk.ericsson.se/artifactory/simple/proj-cpi-release-local/com/ericsson/bss/cpi/jive/jive-tests",
                useTestData: true,
                testdataVersionLocation: 'https://arm.epk.ericsson.se/artifactory/proj-cpi-release-local/com/ericsson/bss/cpi/testdata')

        super.createUmiTestJob( defaultTapasJobPath: 'CPI/UMI%20CPI%20Test',
                                suite: 'suites/umi_test.xml',
                                suiteFile: 'umi_test_\${TARGETHOST}.xml')

        super.createDslJob([])

    }

    @Override
    protected List getRepositories() {
        // Cpi has its own automation for repositories (they use gradle)
        return []
    }
}
