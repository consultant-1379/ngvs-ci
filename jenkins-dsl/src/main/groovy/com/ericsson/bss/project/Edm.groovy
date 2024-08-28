package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.DeployJobBuilder
import com.ericsson.bss.job.GerritSiteJobBuilder
import com.ericsson.bss.job.SiteJobBuilder
import com.ericsson.bss.util.GerritUtil

class Edm extends Project {
    public static String projectName = "edm"
    private static final int INCREASE_TIMEOUT = 60

    public Edm(){
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)

        super.createCreateClusterJob(true)
        super.createRemoveClusterJob()
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-edm.xml"
        integrationTestRepository = gerritUser + "@" + gerritServer + ":29418/edm/com.ericsson.bss.rm.edm.integrationtest"
    }

    @Override
    protected List getRepositories() {
        List repositories = new ArrayList()
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains('com.ericsson.bss.rm.edm')){
                repositories.add(repository)
            }
        }
        repositories.add("jive/edm")

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        if (repository.equals("jive/edm")) {
            repository = "jive-edm"
        }

        String currentJobName = super.getJobName(repository)

        if (currentJobName.contains(projectName + '.')) {
            currentJobName = currentJobName.split(projectName + '.')[1]
        }

        return currentJobName
    }

    @Override
    public void create(parent) {
        super.create(parent)
        super.createUmiTestJob( defaultTapasJobPath: 'EDM/UMI%20EDM%20Test',
                                suite: 'suites/umi_test.xml',
                                suiteFile: 'umi_test_\${TARGETHOST}.xml',
                                useTwoTargethosts: true)

        super.createOvfBuildJob('EDM/Build%20EDM%20\${__VARIANT__}%20OVF', 'suites/build_ovf.xml',
                                'build_ovf_\${TARGETHOST}.xml', ['processor', 'exposure'],
                                ['processor':'EDMEVPROC', 'exposure':'EDMEVEXPO'])

        super.createTargethostInstallJob(
            versionLocation: ['https://arm.epk.ericsson.se/artifactory/proj-edm-release-local/com/ericsson/bss/rm/edm/edmpackage/',
                              'https://arm.epk.ericsson.se/artifactory/proj-edm-release-local/com/ericsson/bss/rm/edm/exposure/integration.package/'],
            defaultTapasJobPath: 'EDM/EDM%20Targethost%20Install',
            useTwoTargethosts: true,
            targethostDescription: ['The machine(s) that should be installed with EDMEVPROC if INSTALLTYPE ' +
                'in [full, processor]. If INSTALLTYPE is exposure this is the processor node exposure will be configured towards.\n' +
                'If multiple machines, use "<b>;</b>" to separate them. Ex. vmx123;vmx456',
                'Host(s) that should be deployed with EDMEVEXPO OVF.' +
                'If multiple machines, use "<b>;</b>" to separate them. Ex. vmx123;vmx456'],
            installType: ['full', 'processor', 'exposure'],
            valuesOfResourceProfiles: ['TeamMachine':[3 * ALLOCATED_CPU_IN_CORE, 12 * ALLOCATE_MEMORY_IN_GIGABITE,
                                                      3 * ALLOCATED_CPU_IN_CORE, 12 * ALLOCATE_MEMORY_IN_GIGABITE],
                                       'TestSystem':[4 * ALLOCATED_CPU_IN_CORE, 16 * ALLOCATE_MEMORY_IN_GIGABITE,
                                                     4 * ALLOCATED_CPU_IN_CORE, 16 * ALLOCATE_MEMORY_IN_GIGABITE],
                                       'Default':['', '', '', '']],
            ovfPacName: ['EDMEVPROC', 'EDMEVEXPO'],
            useDvFile: true,
            useMultipleCils: true)
        super.createGerritCodeFreezeJob()
    }

    @Override
    protected void createDeploy() {

        if (jobName.equalsIgnoreCase('com_ericsson_bss_rm_edm') || jobName.equalsIgnoreCase('exposure')) {

            out.println("createDeploy()")
            DeployJobBuilder deployJobBuilder = new DeployJobBuilder(
                    workspacePath: workspacePath,
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    releaseGoal: releaseGoal,
                    releaseDryrunGoal: releaseDryrunGoal,
                    jobName: folderName + "/" + jobName + "_deploy",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    projectName: projectName,
                    timeoutForJob: INCREASE_TIMEOUT,
                    dslFactory: dslFactory,
                    blameMailList: blameMailList,
                    prepareArm2Gask: prepareArm2Gask
                    )

            deployJobBuilder.build()
        }
        else {
            super.createDeploy()
        }
    }

    @Override
    protected void createReleaseDeploy() {

        if (jobName.equalsIgnoreCase('com_ericsson_bss_rm_edm') || jobName.equalsIgnoreCase('exposure')) {
            out.println("createReleaseDeploy()")

            def releaseProjects = getReleaseBranches()

            if (releaseProjects.size() != 0 && releaseProjects != null) {
                releaseProjects.each {
                    def branchName = it
                    out.println('[INFO] Create release branch for: ' + branchName)
                    DeployJobBuilder releaseBranchDeployJobBuilder = new DeployJobBuilder(
                            workspacePath: workspacePath,
                            gerritUser: gerritUser,
                            gerritServer: gerritServer,
                            releaseGoal: releaseGoal,
                            releaseDryrunGoal: releaseDryrunGoal,
                            jobName: folderName + "/release/" + jobName + "_" + branchName.replace('/', '_'),
                            mavenRepositoryPath: mavenRepositoryPath,
                            mavenSettingsFile: mvnSettingFile,
                            gerritName: gerritName,
                            projectName: projectName,
                            timeoutForJob: INCREASE_TIMEOUT,
                            dslFactory: dslFactory
                            )

                    releaseBranchDeployJobBuilder.buildReleaseBranch(branchName)
                }
            }
        }
        else {
            super.createReleaseDeploy()
        }
    }

    @Override
    protected void createSite() {
        List jobsWithIncreasedTimeout = []
        jobsWithIncreasedTimeout.add('com_ericsson_bss_rm_edm')
        jobsWithIncreasedTimeout.add('exposure')

        if (jobsWithIncreasedTimeout.contains(jobName.toLowerCase())) {
            out.println("createSite()")
            SiteJobBuilder siteJobBuilder = new SiteJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_site",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    dslFactory: dslFactory,
                    timeoutForJob: INCREASE_TIMEOUT,
                    generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName)
                    )

            siteJobBuilder.build()
        }
        else {
            super.createSite()
        }
    }

    @Override
    protected void createSiteGerrit() {
        if (jobName.equalsIgnoreCase('com_ericsson_bss_rm_edm')) {
            out.println("createSiteGerrit()")
            GerritSiteJobBuilder gerritSiteJobBuilder = new GerritSiteJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_gerrit_site",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    projectName: projectName,
                    verboseGerritFeedback: verboseGerritFeedback,
                    dslFactory: dslFactory,
                    timeoutForJob: INCREASE_TIMEOUT,
                    generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName)
                    )

            gerritSiteJobBuilder.build()
        }
        else {
            super.createSiteGerrit()
        }
    }
    @Override
    protected List getRepositoriesForCodeFreeze() {
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))
        List<String> repositories = []
        for (repository in output.split(System.lineSeparator())) {
            if (repository.contains("edm/com.ericsson.bss.rm.edm")) {
                repositories.add(repository)
            }
        }

        return repositories
    }

    @Override
    protected String getCodeFreezeApprovers() {
        '''
        Gerrit account ids for user:
        eanbuko Anbukodai K - 3098
        '''
        return "3098"
    }
}
