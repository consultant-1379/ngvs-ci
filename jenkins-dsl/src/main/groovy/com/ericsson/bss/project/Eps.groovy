package com.ericsson.bss.project

import com.ericsson.bss.Project

class Eps extends Project {

    public static String projectName = "eps"

    public Eps() {
        super.projectName = this.projectName
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-eps.xml"
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = []
        repositories.add("eps/com.ericsson.bss.rm.eps")
        repositories.add("eps/com.ericsson.bss.rm.eps.test")
        return repositories
    }

    @Override
    public void create(parent) {
        super.create(parent)
        super.createOvfBuildJob('EPS/Build%20EPS%20\${__VARIANT__}%20OVF',
                                'suites/build_ovf.xml',
                                'build_ovf_\${TARGETHOST}.xml',
                                ['master', 'worker'],
                                ['master':'EPSMASTER', 'worker':'EPSWORKER'])
        super.createUmiTestJob( defaultTapasJobPath: 'EPS/UMI%20EPS%20Test',
                                suite: 'suites/umi_test.xml',
                                suiteFile: 'umi_test_\${TARGETHOST}.xml',
                                useCil: false,
                                useTwoTargethosts: true)
        super.createTargethostInstallJob(
            installNodeName: 'vmx-edm028',
            versionLocation: ['https://arm.epk.ericsson.se/artifactory/proj-eps-release-local/com/ericsson/bss/rm/eps/EPSMASTER/',
                              'https://arm.epk.ericsson.se/artifactory/proj-eps-release-local/com/ericsson/bss/rm/eps/EPSWORKER/'],
            defaultTapasJobPath: 'EPS/EPS%20Targethost%20Install',
            useTwoTargethosts: true,
            targethostDescription: ['The machine that should be installed with EPSMASTER if INSTALLTYPE ' +
                'in [full, master].', 'The machine that should be deployed with EPSWORKER if INSTALLTYPE ' +
                'in [full, worker].'] ,
            installType: ['full', 'master', 'worker'],
            useCil: false,
            valuesOfResourceProfiles: ['TeamMachine':[ALLOCATED_CPU_IN_CORE * 3, ALLOCATE_MEMORY_IN_GIGABITE * 24,
                                                      ALLOCATED_CPU_IN_CORE * 6, ALLOCATE_MEMORY_IN_GIGABITE * 36],
                                       'TestSystem':[ALLOCATED_CPU_IN_CORE * 4, ALLOCATE_MEMORY_IN_GIGABITE * 32,
                                                     ALLOCATED_CPU_IN_CORE * 8, ALLOCATE_MEMORY_IN_GIGABITE * 49],
                                       'Eco':[ALLOCATED_CPU_IN_CORE * 2, ALLOCATE_MEMORY_IN_GIGABITE * 16,
                                              ALLOCATED_CPU_IN_CORE * 4, ALLOCATE_MEMORY_IN_GIGABITE * 24],
                                       'Default': ['', '', '', '']],
            ovfPacName: ['EPSMASTER', 'EPSWORKER'],
            useMultipleCils: true,
            useMultipleTargethosts: false)

    }
}
