package com.ericsson.bss.project

import com.ericsson.bss.util.GerritUtil
import com.ericsson.bss.job.coba.CobaUmiTestJobBuilder
import com.ericsson.bss.job.coba.CobaTargethostInstallJobBuilder
import com.ericsson.bss.Project

class Coba extends Project {
    public static String projectName = "coba"

    public Coba(){
        super.projectName = this.projectName
        super.releaseGoal = "-Dresume=false release:prepare release:perform -DpreparationGoals=\"clean verify install\""
        super.releaseDryrunGoal = "-Dresume=false -DdryRun=true release:prepare -DpreparationGoals=\"clean verify install\""
    }

    @Override
    public void init(parent) {
        super.init(parent)
        super.createCreateClusterJob(false)
        super.createRemoveClusterJob()

        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-coba.xml"
        integrationTestRepository = gerritUser + "@" + gerritServer + ":29418/charging/com.ericsson.bss.rm.coba.integrationtest"
    }

    @Override
    public void create(parent) {
        super.create(parent)
        super.createOvfBuildJob('COBA/Build%20COBA%20OVF', 'suites/build_ovf.xml',
                                'build_ovf_\${TARGETHOST}.xml')
        super.createUmiTestJob( defaultTapasJobPath: 'COBA/UMI%20COBA%20Test',
                                suite: 'suites/umi_test.xml',
                                suiteFile: 'umi_test_\${TARGETHOST}.xml',
                                umiTestJobBuilderClass: CobaUmiTestJobBuilder,
                                timeoutForJob: 240)
        super.createTargethostInstallJob(
                installNodeName: 'vmx-coba004',
                versionLocation: ['https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/COBA/,' +
                                 'https://arm.epk.ericsson.se/artifactory/simple/proj-coba-release-local/com/ericsson/bss/rm/coba/integration/cobapackage/;' +
                                 '1.6.0'],
                defaultTapasJobPath: 'COBA/Targethost%20COBA%20Install',
                targethostInstallJobBuilderClass: CobaTargethostInstallJobBuilder,
                useDvFile: true,
                nrOfNetworks: 3,
                useJiveTests: true,
                jiveMetaData: 'https://arm.epk.ericsson.se/artifactory/simple/proj-coba-release-local/com/ericsson/bss/rm/coba/jive/coba.jiveTest.impl/')
    }

    @Override
    protected void createSite() {
        if (jobName == "gui"){
            out.println("[INFO] Build timeout set to 30 for gui_site")
            super.createSite(30)
        } else {
            super.createSite()
        }
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = new ArrayList()
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains('charging/com.ericsson.bss.rm.coba.')) {
                repositories.add(repository)
            }
        }

        repositories.remove('charging/com.ericsson.bss.rm.coba.devenv')
        repositories.remove('charging/com.ericsson.bss.rm.coba.umi')
        repositories << "eftf/coba"
        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        if (repository.equals("eftf/coba")) {
            repository = "eftf-coba"
        }
        String currentJobName = super.getJobName(repository)

        if (currentJobName.contains(projectName + '.')) {
            currentJobName = currentJobName.split(projectName + '.')[1]
        }

        return currentJobName
    }
}
