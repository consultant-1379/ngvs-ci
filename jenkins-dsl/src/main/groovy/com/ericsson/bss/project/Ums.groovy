package com.ericsson.bss.project

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.Project
import com.ericsson.bss.util.GerritUtil

class Ums extends Project {
    public static String projectName = "ums"
    private String releaseParametersForIntegrationTest = ' -DpreparationGoals="install" -Darguments="' + AbstractJobBuilder.MVN_SETTINGS + ' ' +
            AbstractJobBuilder.MVN_REPOSIOTRY + '"'

    public Ums(){
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)

        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-ums.xml"
    }

    @Override
    public void create(parent) {
        super.create(parent)

        HashMap<String, List> VALUES_OF_RESOURCE_PROFILES = [
                                                                'TeamMachine': [ALLOCATED_CPU_IN_CORE * 2, ALLOCATE_MEMORY_IN_GIGABITE * 4],
                                                                'TestSystem': [ALLOCATED_CPU_IN_CORE * 2, ALLOCATE_MEMORY_IN_GIGABITE * 4],
                                                                'Washingmachine': [ALLOCATED_CPU_IN_CORE * 2, ALLOCATE_MEMORY_IN_GIGABITE * 4],
                                                                'Default': ['', '']
                                                            ]
        super.createTargethostInstallJob(
            versionLocation: ['https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/UMS/'],
            defaultTapasJobPath: 'UMS/Targethost%20UMS%20Install',
            valuesOfResourceProfiles: VALUES_OF_RESOURCE_PROFILES,
            useDvFile: true
        )

        super.createCreateClusterJob()
        super.createRemoveClusterJob()
    }

    @Override
    protected void createDeploy() {

        if (jobName.contains('agent')) {
            releaseGoal = DEFAULT_RELEASE_GOAL + ' ' + releaseParametersForIntegrationTest
            releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL + ' ' + releaseParametersForIntegrationTest
        }

        super.createDeploy()
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = []
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains('com.ericsson.bss.rm.ums.')) {
                repositories.add(repository)
            }
        }

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        String currentJobName = super.getJobName(repository)

        currentJobName = currentJobName.replace('com.ericsson.bss.rm.ums.', '')

        return currentJobName
    }
}
