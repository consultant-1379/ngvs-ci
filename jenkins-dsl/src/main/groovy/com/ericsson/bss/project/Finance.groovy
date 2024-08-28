package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.DeployJobBuilder
import com.ericsson.bss.util.GerritUtil

class Finance extends Project {
    public static String projectName = "finance"

    public Finance(){
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)

        super.createCreateClusterJob(false)
        super.createRemoveClusterJob()
        integrationTestRepository = gerritUser + "@" + gerritServer + ":29418/finance/com.ericsson.bss.rm.finance.integrationtest"
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-finance.xml"
    }

    @Override
    public void create(parent) {
        super.create(parent)
        super.createOvfBuildJob('Finance/Build%20Finance%20OVF', 'suites/build_ovf.xml',
                                'build_ovf_\${TARGETHOST}.xml')
        super.createUmiTestJob( defaultTapasJobPath: 'Finance/UMI%20Finance%20Test',
                                suite: 'suites/umi_test.xml',
                                suiteFile: 'umi_test_\${TARGETHOST}.xml')
        super.createTargethostInstallJob(
            versionLocation: ['https://arm.epk.ericsson.se/artifactory/proj-finance-release-local/com/ericsson/bss/rm/finance/opd/finance/'],
            defaultTapasJobPath: 'Finance/Finance%20Targethost%20Install',
            useDvFile: true,
            useTestData: true)
        super.createCilTargethostRollbackJob()
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = new ArrayList()
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains('com.ericsson.bss.rm.finance')){
                repositories.add(repository)
            }
        }

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        String currentJobName = super.getJobName(repository)

        currentJobName = currentJobName.replace('com.ericsson.bss.rm.finance.', '')

        return currentJobName
    }

    @Override
    protected void createDeploy() {
        if (jobName.equalsIgnoreCase('integrationtest')) {
            out.println("createDeploy()")
            DeployJobBuilder deployJobBuilder = new DeployJobBuilder(
                    workspacePath: workspacePathMesos,
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    releaseGoal: DEFAULT_RELEASE_GOAL + " " + PUBLISH_TO_JIVE_GOAL,
                    releaseDryrunGoal: DEFAULT_DRYRUN_RELEASE_GOAL,
                    jobName: folderName + "/" + jobName + "_deploy",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    projectName: projectName,
                    dslFactory: dslFactory,
                    blameMailList: blameMailList,
                    prepareArm2Gask: prepareArm2Gask,
                    generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                    extraMavenParameters: PUBLISH_TO_JIVE_GOAL
            )
            deployJobBuilder.build()
        } else {
            super.createDeploy()
        }
    }

    @Override
    protected List getIgnoredRepositoriesForIntegrationTest() {
        List ignoredRepositories = super.getIgnoredRepositoriesForIntegrationTest()

        ignoredRepositories.add('finance/com.ericsson.bss.rm.finance.ui')

        return ignoredRepositories
    }
}
