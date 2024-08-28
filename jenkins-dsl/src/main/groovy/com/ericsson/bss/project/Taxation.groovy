package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.util.GerritUtil
import com.ericsson.bss.job.MultiRepositoryReleaseJobBuilder

class Taxation extends Project {
    public static String projectName = "taxation"

    public Taxation(){
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)
        integrationTestRepository = gerritUser + "@" + gerritServer + ":29418/taxation/com.ericsson.bss.rm.taxation.integrationtest"
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-taxation.xml"
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = []
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains('com.ericsson.bss.rm.taxation')){
                repositories.add(repository)
            }
        }

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        String currentJobName = super.getJobName(repository)

        currentJobName = currentJobName.replace('com.ericsson.bss.rm.taxation.', '')

        return currentJobName
    }

    @Override
    public void create(parent) {
        super.create(parent)
        createMultiRepositoryReleaseJob()
    }

    private void createMultiRepositoryReleaseJob() {
        out.println("createMultiRepositoryReleaseJob()")

        List repositoryList = []
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/taxation/com.ericsson.bss.rm.taxation.top')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/taxation/com.ericsson.bss.rm.taxation.common')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/taxation/com.ericsson.bss.rm.taxation.oam')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/taxation/com.ericsson.bss.rm.taxation.versioning')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/taxation/com.ericsson.bss.rm.taxation.persistence')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/taxation/com.ericsson.bss.rm.taxation.dataaccess')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/taxation/com.ericsson.bss.rm.taxation.cdac')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/taxation/com.ericsson.bss.rm.taxation.vre')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/taxation/com.ericsson.bss.rm.taxation.rest')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/taxation/com.ericsson.bss.rm.taxation.testdata')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/taxation/com.ericsson.bss.rm.taxation.productiondependencies')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/taxation/com.ericsson.bss.rm.taxation.integration')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/taxation/com.ericsson.bss.rm.taxation.integrationtest')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/taxation/com.ericsson.bss.rm.taxation.jive')

        MultiRepositoryReleaseJobBuilder releaseJobBuilder = new MultiRepositoryReleaseJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                projectName: projectName,
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                repositoryList: repositoryList,
                buildName: 'taxation-release',
                releaseRepository: 'proj-taxation-release-local',
                stagingRepository: 'proj-taxation-staging-local',
                mail: 'shashank.g.gupta@ericsson.com',
                dslFactory: dslFactory
                )
        super.createTargethostInstallJob(
            versionLocation: ['https://arm.epk.ericsson.se/artifactory/proj-taxation-dev/com/ericsson/bss/rm/taxation/integration/taxationpackage/'],
            defaultTapasJobPath: 'Taxation/Taxation%20Targethost%20Install',
            useDvFile: true,
            valuesOfResourceProfiles: ['TeamMachine':[ALLOCATED_CPU_IN_CORE * 4, ALLOCATE_MEMORY_IN_GIGABITE * 8],
                                       'TestSystem':[ALLOCATED_CPU_IN_CORE * 4, ALLOCATE_MEMORY_IN_GIGABITE * 8],
                                       'Default': ['', '']])
        releaseJobBuilder.build()
    }

    @Override
    protected List getIgnoredRepositoriesForIntegrationTest() {
        List ignoredRepositories = super.getIgnoredRepositoriesForIntegrationTest()
        ignoredRepositories.remove('integration')
        return ignoredRepositories
    }
}
