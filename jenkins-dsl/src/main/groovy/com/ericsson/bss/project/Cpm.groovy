package com.ericsson.bss.project

import com.ericsson.bss.job.cpm.CpmDslJobBuilder
import com.ericsson.bss.job.cpm.CpmSiteJobBuilder
import com.ericsson.bss.job.cpm.CpmUmiTestJobBuilder
import com.ericsson.bss.job.cpm.CpmWashingMachineReleaseBranchJobBuilder
import com.ericsson.bss.job.DeployJobBuilder
import com.ericsson.bss.job.GerritIntegrationTestJobBuilder
import com.ericsson.bss.job.SonarJobBuilder

import com.ericsson.bss.Project
import com.ericsson.bss.util.GerritUtil

class Cpm extends Project {
    private static final int CPM_INTEGRATION_TEST_TIMEOUT = 30
    private static final int CPM_CORE_SONAR_TIMEOUT = 20

    public Cpm(){
        super.releaseGoal = "-Dresume=false -Dgoals=deploy release:prepare release:perform"
        super.releaseDryrunGoal = "-Dresume=false -DdryRun=true -Dgoals=deploy release:prepare"
    }

    @Override
    public void init(parent) {
        super.init(parent)
        super.createCreateClusterJob(true)
        super.createRemoveClusterJob()
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-cpm.xml"
        integrationTestRepository = gerritUser + "@" + gerritServer + ":29418/charging/com.ericsson.bss.rm.cpm.integrationtest"
        secondIntegrationTestRepository = gerritUser + "@" + gerritServer + ":29418/charging/com.ericsson.bss.rm.cpm.cdac.integrationtest"
    }

    @Override
    public boolean runProject(String projectName){
        this.projectName = projectName
        super.projectName = this.projectName
        if (projectName == "cpm") {
            return true
        }
        return false
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = new ArrayList()
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains('charging/') && repository.contains(projectName + '.')) {
                repositories.add(repository)
            }
        }
        repositories.remove("charging/com.ericsson.bss.rm.cpm.restapi.rmdataaccess")
        repositories << "eftf/cpm"
        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        if (repository.equals("eftf/cpm")) {
            repository = "eftf-cpm"
        }
        String currentJobName = super.getJobName(repository)
        if (currentJobName.contains(projectName + '.')) {
            currentJobName = currentJobName.split(projectName + '.')[1]
        } else {
            currentJobName = currentJobName.replace('com.ericsson.bss.charging.', '')
        }

        return currentJobName
    }

    @Override
    protected List getGuiRepositories() {
        List<String> repositories = new ArrayList()
        repositories.add("ui")
        return repositories
    }

    @Override
    public void create(parent) {
        super.create(parent)
        super.createUmiTestJob( defaultTapasJobPath: 'Cpm/UMI%20Cpm%20Test',
                                suite: 'suites/umi_test.xml',
                                suiteFile:  'umi_test_\${TARGETHOST}.xml',
                                extraDescription: '\n\n See <a href="http://cpm.epk.ericsson.se/wiki/index.php/TE_Servers">' +
                                                  'TE_Servers</a> on what to do prior to running this job.<p>',
                                withInstallNodePool: true,
                                installNodePool: 'vmx-cpmka-026,vmx-cpmka-036',
                                umiTestJobBuilderClass: CpmUmiTestJobBuilder,
                                )

        super.createReleaseBranchWashingMachineJobBuilder(false, true)
        super.removeReleaseBranchWashingMachineJobBuilder()
        super.createOvfBuildJob('CPM/Build%20CPM%20OVF', 'suites/installnode/build_ovf.xml',
                                'build_ovf_\${TARGETHOST}.xml')
        super.createWashingMachineReleaseBranch(
            washingMachineReleaseBranchJobBuilderClass: CpmWashingMachineReleaseBranchJobBuilder,
            mailingList: 'cpm_washingmachine@mailman.lmera.ericsson.se',
            defaultTapasJobPath: 'CPM/CPM%20Washingmachine%20Releasebranch',
            suite: 'suites/installnode/washingmachine_branch.xml',
            useRpmWm: false,
            useGitBranch: true
        )

        super.createGerritCodeFreezeJob()
    }

    protected void createDeploy() {
        if (jobName.equalsIgnoreCase('integrationtest')) {
            out.println("createDeploy()")
            DeployJobBuilder deployJobBuilder = new DeployJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    releaseGoal: releaseGoal,
                    releaseDryrunGoal: releaseDryrunGoal,
                    jobName: folderName + "/" + jobName + "_deploy",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    projectName: projectName,
                    dslFactory: dslFactory,
                    blameMailList: blameMailList,
                    timeoutForJob: getDefaultJobTimeout(),
                    extraMavenParameters: "-DpublishToJivePortal=true"
                    )

            deployJobBuilder.build()
        }
        else {
            super.createDeploy()
        }
    }

    @Override
    protected void createIntegrationTestGerrit() {
        out.println("createIntegrationTestGerrit()")
        def integrationTestToRepository = jobName in [
            'bundle',
            'devenv',
            'eftf-cpm',
            'integrationtest',
            "test_integration",
            'jive',
            'top',
            'umi',
            'variationpoint'
        ]

        def hasSecondIntegrationTestRepository = jobName in [
            'cdac_common',
            'cdac_translation',
            'cdac_dataenquiry'
        ]

        if (!integrationTestToRepository && integrationTestRepository != null) {
            GerritIntegrationTestJobBuilder gerritIntegrationTestJobBuilder = new GerritIntegrationTestJobBuilder(
                    workspacePath: workspacePath,
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_gerrit_integration_test",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    projectName: projectName,
                    integrationTestRepository: integrationTestRepository,
                    timeoutForJob: CPM_INTEGRATION_TEST_TIMEOUT,
                    dslFactory: dslFactory
                    )

            if (jobName == "ui") {
                gerritIntegrationTestJobBuilder.enabled = false
            }
            gerritIntegrationTestJobBuilder.build()
            if (hasSecondIntegrationTestRepository) {
                gerritIntegrationTestJobBuilder.addAdditionalIntegrationTestConfig(secondIntegrationTestRepository)
            }
        }
    }

    @Override
    protected void createDslJob(List repositories) {
        out.println("createDslJobs()")
        CpmDslJobBuilder createDslJobBuilder = new CpmDslJobBuilder(
                jenkinsURL:jenkinsURL,
                gerritUser: GERRIT_EPK_USER,
                gerritServer: GERRIT_EPK_SERVER,
                gitRepositoryName: 'tools/eta/jenkins-dsl',
                gerritName: projectName,
                jobName: projectName + '_dsl',
                repositories: repositories,
                dslFactory: dslFactory
                )

        createDslJobBuilder.build()
    }

    @Override
    protected void createSite(){
        out.println("createSite()")
        CpmSiteJobBuilder cpmSiteJobBuilder = new CpmSiteJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_site",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                dslFactory: dslFactory,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName)
                )

        cpmSiteJobBuilder.build()
    }

    @Override
    protected int getDefaultSonarJobTimeout() {
        if (jobName.equalsIgnoreCase('core')) {
            return CPM_CORE_SONAR_TIMEOUT
        } else {
            return super.getDefaultSonarJobTimeout()
        }
    }

    @Override
    protected int getDefaultJobTimeout() {
        if (jobName.equalsIgnoreCase('integrationtest')) {
            return CPM_INTEGRATION_TEST_TIMEOUT
        }
        else {
            return super.getDefaultJobTimeout()
        }
    }

    @Override
    protected void createSonar() {
        int timeoutForJob = 20
        if (jobName.equalsIgnoreCase('core')) {
            out.println("createSonar()")
            SonarJobBuilder sonarJobBuilder = new SonarJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_sonar",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    dslFactory: dslFactory,
                    generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                    timeoutForJob: timeoutForJob
                    )
            sonarJobBuilder.build()
        } else {
            super.createSonar()
        }
    }

    @Override
    protected List getRepositoriesForCodeFreeze() {
        List<String> repositories = []

        repositories.add("charging/com.ericsson.bss.rm.cpm.top")
        repositories.add("charging/com.ericsson.bss.rm.cpm.bundle")
        repositories.add("charging/com.ericsson.bss.rm.cpm.devenv")
        repositories.add("charging/com.ericsson.bss.rm.cpm.site")
        repositories.add("charging/com.ericsson.bss.rm.cpm.core")
        repositories.add("charging/com.ericsson.bss.rm.cpm.productiondependencies")
        repositories.add("charging/com.ericsson.bss.rm.cpm.jive")
        repositories.add("charging/com.ericsson.bss.rm.cpm.umi")
        repositories.add("charging/com.ericsson.bss.rm.cpm.integration")
        repositories.add("charging/com.ericsson.bss.rm.cpm.integrationtest")
        repositories.add("charging/com.ericsson.bss.rm.cpm.cdac.site")
        repositories.add("charging/com.ericsson.bss.rm.cpm.cdac.common")
        repositories.add("charging/com.ericsson.bss.rm.cpm.cdac.translation")
        repositories.add("charging/com.ericsson.bss.rm.cpm.cdac.dataenquiry")
        repositories.add("charging/com.ericsson.bss.rm.cpm.cdac.integrationtest")
        repositories.add("charging/com.ericsson.bss.rm.cpm.restapi.common")
        repositories.add("charging/com.ericsson.bss.rm.cpm.restapi.idtranslation")
        repositories.add("charging/com.ericsson.bss.rm.cpm.restapi.datamanagement")
        repositories.add("charging/com.ericsson.bss.rm.cpm.restapi.business")
        repositories.add("charging/com.ericsson.bss.rm.cpm.restapi.flowmanagement")
        repositories.add("charging/com.ericsson.bss.rm.cpm.restapi.specificationmanagement")
        repositories.add("charging/com.ericsson.bss.rm.cpm.ui")
        repositories.add("charging/com.ericsson.bss.rm.cpm.ui.rmdataaccess")
        repositories.add("charging/com.ericsson.bss.rm.cpm.variationpoint")

        return repositories
    }

    @Override
    protected String getCodeFreezeApprovers() {
        '''
        Gerrit account ids for user:
        Shailendra Kumar Chouksey - 2661
        Amit Bathla - 1082
        Pratibha Singh S - 2168
        '''
        return "2661 1082 2168"
    }
}
