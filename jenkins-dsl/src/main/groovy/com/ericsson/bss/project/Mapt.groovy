package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.MultiRepositoryReleaseJobBuilder
import com.ericsson.bss.util.GerritUtil
import com.ericsson.bss.job.mapt.MaptTargethostInstallJobBuilder

class Mapt extends Project {
    public static String projectName = "mapt"
    protected HashMap<String, List> valuesOfResourceProfiles = ['TestSystem':['2', '12288'], 'Default':['', '']]

    public Mapt(){
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)

        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-maptranslator.xml"
        integrationTestRepository = gerritUser + "@" + gerritServer + ":29418/maptranslator/com.ericsson.bss.rm.maptranslator.integrationtest"
    }

    @Override
    public void create(Object parent) {
        super.create(parent)
        createMultiRepositoryReleaseJob()
        super.createCreateClusterJob(false)
        super.createRemoveClusterJob()
        super.createOvfBuildJob('MAPT/Build%20MAPT%20OVF', 'suites/build_ovf.xml',
                                'build_ovf_\${TARGETHOST}.xml')
        super.createUmiTestJob( defaultTapasJobPath: 'MAPT/UMI%20MAPT%20Test',
                                suite: 'suites/umi_test.xml',
                                suiteFile: 'umi_test_\${TARGETHOST}.xml')

        super.createTargethostInstallJob(
                versionLocation: ['https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/MAPT/,' +
                                 'https://arm.epk.ericsson.se/artifactory/proj-maptranslator-release/com/ericsson/bss/rm/maptranslator/integration/' +
                                     'maptpackage/;1.6.0'],
                defaultTapasJobPath: 'MAPT/Targethost%20MAPT%20Install',
                targethostInstallJobBuilderClass: MaptTargethostInstallJobBuilder,
                valuesOfResourceProfiles: valuesOfResourceProfiles,
                useCil: false,
                nrOfNetworks: 3,
                useDvFile: true,
                useJiveTests: true,
                jiveMetaData: 'https://arm.epk.ericsson.se/artifactory/api/storage/proj-maptranslator-release-local/jive/jive-mapt/')
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = new ArrayList()
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains('com.ericsson.bss.rm.maptranslator')){
                repositories.add(repository)
            }
        }
        repositories.add("jive/mapt")

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        if (repository.equals("jive/mapt")) {
            repository = "jive-mapt"
        }

        String currentJobName = super.getJobName(repository)

        currentJobName = currentJobName.replace('com.ericsson.bss.rm.', '')

        return currentJobName
    }

    private void createMultiRepositoryReleaseJob() {
        out.println("createMultiRepositoryReleaseJob()")

        List repositoryList = []
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/maptranslator/com.ericsson.bss.rm.maptranslator')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/maptranslator/com.ericsson.bss.rm.maptranslator.bundle')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/maptranslator/com.ericsson.bss.rm.maptranslator.integration')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/maptranslator/com.ericsson.bss.rm.maptranslator.integration.config')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/maptranslator/com.ericsson.bss.rm.maptranslator.productiondependencies')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/maptranslator/com.ericsson.bss.rm.maptranslator.cli')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/maptranslator/com.ericsson.bss.rm.maptranslator.integrationtest')

        MultiRepositoryReleaseJobBuilder releaseJobBuilder = new MultiRepositoryReleaseJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                projectName: projectName,
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                repositoryList: repositoryList,
                buildName: 'maptranslator-release',
                releaseRepository: 'proj-maptranslator-release-local',
                stagingRepository: 'proj-maptranslator-staging-local',
                mail: 's.m.sithik@ericsson.com',
                dslFactory: dslFactory
                )

        releaseJobBuilder.build()
    }

    @Override
    protected void createSite() {
        int timeoutSite = getDefaultJobTimeout()
        if (jobName == 'maptranslator') {
            timeoutSite = 120
        }
        super.createSite(timeoutSite)
    }

    @Override
    protected void createSiteGerrit() {
        int timeoutSite = getDefaultJobTimeout()
        if (jobName == 'maptranslator') {
            timeoutSite = 120
        }
        super.createSite(timeoutSite)
    }

    @Override
    protected List getIgnoredRepositoriesForIntegrationTest() {
        return [
                'maptranslator_integrationtest',
                'jive-mapt'
        ]
    }
}
