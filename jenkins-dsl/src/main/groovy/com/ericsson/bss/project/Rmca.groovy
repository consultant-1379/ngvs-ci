package com.ericsson.bss.project

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.Project
import com.ericsson.bss.job.AutoAddReviewerJobBuilder
import com.ericsson.bss.job.BackwardCompatibilityTestJobBuilder
import com.ericsson.bss.job.CreateBranchJobBuilder
import com.ericsson.bss.job.DeployJobBuilder
import com.ericsson.bss.job.GerritDependencyTestJobBuilder
import com.ericsson.bss.job.GerritSiteJobBuilder
import com.ericsson.bss.job.GerritSonarJobBuilder
import com.ericsson.bss.job.IntegrationTestJobBuilder
import com.ericsson.bss.job.rmca.RmcaAddRemoveNightlyFullTargethostInstallJobBuilder
import com.ericsson.bss.job.rmca.RmcaDeployJobBuilder
import com.ericsson.bss.job.rmca.RmcaGerritSeleniumJobBuilder
import com.ericsson.bss.job.rmca.RmcaGerritSonarJobBuilder
import com.ericsson.bss.job.rmca.RmcaGerritUnitTestJobBuilder
import com.ericsson.bss.job.rmca.RmcaReleaseBranchDeployJobBuilder
import com.ericsson.bss.job.rmca.RmcaReleasePrepareJobBuilder
import com.ericsson.bss.job.rmca.RmcaSeleniumJobBuilder
import com.ericsson.bss.job.rmca.RmcaSonarJobBuilder
import com.ericsson.bss.job.rmca.RmcaUmiTestJobBuilder
import com.ericsson.bss.job.rmca.RmcaVideoDeployJobBuilder
import com.ericsson.bss.job.rmca.RmcaTargethostInstallJobBuilder
import com.ericsson.bss.job.SiteJobBuilder
import com.ericsson.bss.job.SonarJobBuilder

import javaposse.jobdsl.dsl.Job

class Rmca extends Project {

    public static String projectName = 'rmca'

    private static final String GUI_PROFILE = 'gui'
    private static final String SELENIUM_PROFILE = 'selenium'
    private static final String SELENIUM_PROFILE_WITHOUT_CUCUMBER = SELENIUM_PROFILE + ',!cucumber'
    private static final String SELENIUM_CUCUMBER_PROFILE = 'cucumber'
    private static final String SELENIUM_JIVE_PROFILE = 'jive'
    private static final String EXTRA_RELEASE_PARAMETERS = ' -P' + GUI_PROFILE + ' -DuseReleaseProfile=false -DpreparationGoals="install" -Darguments="-P' +
            GUI_PROFILE + ' -Dorg.ops4j.pax.url.mvn.settings=\${MAVEN_SETTINGS} -Dorg.ops4j.pax.url.mvn.localRepository=\${MAVEN_REPOSITORY} ' +
            '--settings \${MAVEN_SETTINGS} -Dmaven.repo.local=\${MAVEN_REPOSITORY}"'

    private static final String RELEASE_GOAL = DEFAULT_RELEASE_GOAL + ' -Dgoals=deploy' + EXTRA_RELEASE_PARAMETERS
    private static final String RELEASE_DRYRUN_GOAL = DEFAULT_DRYRUN_RELEASE_GOAL + EXTRA_RELEASE_PARAMETERS

    private Map extraEnvironmentVariables
    private String jiveMetaData = 'https://arm.epk.ericsson.se/artifactory/simple/proj-rmca-release-local/com/ericsson/bss/rmca/jiveTest/'
    private String seleniumMetaData = 'https://arm.epk.ericsson.se/artifactory/simple/proj-rmca-release-local/com/ericsson/bss/rmca/seleniumtest/'
    private String versionLocation = 'https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/RMCA/,' +
            'https://arm.epk.ericsson.se/artifactory/simple/proj-rmca-release-local/com/ericsson/bss/rmca/integration/rmcapackage/;24.2.0'
    private int timeout = 45
    private int sonarTimeout = 45
    private int deployTimeout = 90
    private boolean generateGUIconfig = true
    private boolean symlinkWorkspace = true
    private boolean codeReviewTrigger = true

    protected HashMap<String, List> valuesOfResourceProfiles = ['TestSystem':[ALLOCATED_CPU_IN_CORE, ALLOCATE_MEMORY_IN_GIGABITE * 4],
                                                                'Extended':[ALLOCATED_CPU_IN_CORE * 2, ALLOCATE_MEMORY_IN_GIGABITE * 8],
                                                                'Washingmachine':[ALLOCATED_CPU_IN_CORE * 4, ALLOCATE_MEMORY_IN_GIGABITE * 8],
                                                                'Default':['', '']]
    protected List<String> resourceProfiles = valuesOfResourceProfiles.keySet().toList()

    public Rmca(){
        super.projectName = this.projectName

        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-rmca.xml"

        overrideJvmOptions()
    }

    @Override
    public void init(parent) {
        super.init(parent)
    }

    @Override
    protected List getRepositories() {
        List repositories = []
        repositories.add("rmca/com.ericsson.bss.rmca")
        repositories.add("rmca/com.ericsson.bss.rmca.configurations")
        repositories.add("rmca/com.ericsson.bss.rmca.oam")
        repositories.add("eftf/rmca")

        out.println("repositories: " + repositories)
        return repositories
    }

    @Override
    protected List getRepositoriesForGerritJive() {
        List allRepositories =  getRepositories()
        List ignoreRepositories =  ignoreRepository()
        List gerritJiveRepositories = []
        allRepositories.each {
            if (!(it in ignoreRepositories)) {
                gerritJiveRepositories.add(it)
            }
        }
        out.println("Repositories for gerrit jive: " + gerritJiveRepositories)
        return gerritJiveRepositories
    }

    @Override
    protected String getJobName(String repository) {
        if (repository == "eftf/rmca") {
            repository = "eftf-rmca"
        }
        String currentJobName = super.getJobName(repository)

        if (currentJobName.contains(projectName + '.')) {
            currentJobName = currentJobName.split(projectName + '.')[1]
        } else {
            currentJobName = currentJobName.replace('com.ericsson.bss.', '')
        }

        return currentJobName
    }

    @Override
    public void create(parent){
        super.create(parent)
        createReleasePrepareJob()
        super.createUpgradeMsvCil('eforge')
        super.createUpgradeGerritJiveMsvCil('eforge')
        super.createGerritJiveClusterUpgradeJob()
        super.createInstallRpmJob("RMCA/RMCA%20Targethost%20Install%20RPM")
        super.createReleaseBranchWashingMachineJobBuilder(false)
        super.removeReleaseBranchWashingMachineJobBuilder(true)
        super.createWashingMachineReleaseBranch(
            mailingList: 'rmca_washingmachine@mailman.lmera.ericsson.se',
            defaultTapasJobPath: 'RMCA/RMCA%20Washingmachine%20Releasebranch',
            suite: 'suites/washingmachine_branch.xml',
            rpmSuite: 'suites/washingmachine_rpm.xml',
            useRpmWm: true,
            timeoutForJobRpm: 120
        )
        super.createUmiTestJob( defaultTapasJobPath: 'RMCA/UMI%20RMCA%20Test',
                                suite: 'suites/umi_test.xml',
                                suiteFile: 'umi_test_\${TARGETHOST}.xml',
                                umiTestJobBuilderClass: RmcaUmiTestJobBuilder)
        super.createTargethostInstallJob(
            installNodeName: 'vmx-rmca150',
            versionLocation: [versionLocation],
            defaultTapasJobPath: 'RMCA/Targethost%20RMCA%20Install',
            valuesOfResourceProfiles: valuesOfResourceProfiles,
            targethostInstallJobBuilderClass: RmcaTargethostInstallJobBuilder,
            useJiveTests: true,
            jiveMetaData: jiveMetaData,
            useSeleniumTests: true,
            seleniumMetaData: seleniumMetaData,
            useDvFile: true
        )
        super.createEpValidatorJob(
            versionLocation: versionLocation,
            defaultTapasJobPath: 'RMCA/RMCA%20EP%20Verification',
            valuesOfResourceProfiles: valuesOfResourceProfiles,
            resourceProfiles: resourceProfiles,
            useJiveTests: true,
            jiveMetaData: jiveMetaData,
            useSeleniumTests: true,
            seleniumMetaData: seleniumMetaData,
            useDvFile: true
        )
        super.createNightlyFullTargethostInstall(
            cronTrigger: 1,
            msvResourceProfile: "TestSystem",
            cilResourceProfile: "TestSystem",
            targethostInstallParameters:
            [
                "TARGETHOST": "targethost",
                "RUN_SIMULATORS": "runsimulators"
            ]
        )
        super.createAddRemoveNightlyFullTargethostInstall(
            addRemoveNightlyFullTargethostInstallJobBuilderClass: RmcaAddRemoveNightlyFullTargethostInstallJobBuilder,
            valuesOfResourceProfiles: valuesOfResourceProfiles
        )
        super.createSetMsvCilVersion()
        super.createAddRemoveNightlyWashingmachineMsvCilUpgrade(false)
        super.createNightlyWashingmachineMsvCilUpgradeJob(
            msvResourceProfile: "Washingmachine",
            cilResourceProfile: "Washingmachine"
        )

        super.createGerritCodeFreezeJob()
    }

    private List ignoreRepository() {
        return [
            'rmca/com.ericsson.bss.rmca.configurations',
            'rmca/com.ericsson.bss.rmca.oam',
            'eftf/rmca'
        ]
    }

    @Override
    protected void createForRepository() {
        super.createForRepository()
        boolean ignoredRepository = gerritName in ignoreRepository()
        if (!ignoredRepository) {
            createSelenium()
            createSeleniumGerrit()
            createFeatureDeploy()
            createVideoDeploy()
            createBackwardCompatibilityTest()
            createIntegrationTest()
        }
    }

    @Override
    protected createJiveTestGerrit() {
        super.createJiveTestGerrit("scripts/rmca_gerrit_deploy_to_karaf_then_jive.sh", 90)
    }

    @Override
    protected void createFolders() {
        out.println("createFolders()")
        super.createFolders()
        folderName = projectName + "/" + jobName
        dslFactory.folder(folderName + "/feature") {}
    }

    @Override
    protected void createSonar() {
        out.println("createSonar()")
        overrideJvmOptions()
        SonarJobBuilder sonarJobBuilder = new RmcaSonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                timeoutForJob: sonarTimeout,
                generateGUIconfig: this.generateGUIconfig,
                extraEnvironmentVariables: this.extraEnvironmentVariables,
                profilesToBeUsed: GUI_PROFILE + ',' + SELENIUM_PROFILE,
                dslFactory: dslFactory
                )

        sonarJobBuilder.build()
    }

    @Override
    protected void createReleaseSonar() {
        out.println("createReleaseSonar()")

        def releaseProjects = getReleaseBranches()

        if (releaseProjects.size() != 0 && releaseProjects != null) {
            releaseProjects.each {
                def branchName = it
                out.println('[INFO] Create sonar job for: ' + branchName)
                SonarJobBuilder sonarJobBuilder = new RmcaSonarJobBuilder(
                        gerritUser: gerritUser,
                        gerritServer: gerritServer,
                        jobName: folderName + "/release/" + jobName + "_" + branchName.replace('/', '_') + '_sonar',
                        mavenRepositoryPath: mavenRepositoryPath,
                        mavenSettingsFile: mvnSettingFile,
                        gerritName: gerritName,
                        timeoutForJob: sonarTimeout,
                        generateGUIconfig: this.generateGUIconfig,
                        extraEnvironmentVariables: this.extraEnvironmentVariables,
                        profilesToBeUsed: GUI_PROFILE + ',' + SELENIUM_PROFILE,
                        dslFactory: dslFactory,
                        branchName: branchName
                        )

                sonarJobBuilder.build()
            }
        }
    }

    @Override
    protected void createSite() {
        out.println("createSite()")
        SiteJobBuilder siteJobBuilder = new SiteJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_site",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                timeoutForJob: deployTimeout,
                generateGUIconfig: this.generateGUIconfig,
                extraEnvironmentVariables: this.extraEnvironmentVariables,
                profilesToBeUsed: GUI_PROFILE,
                dslFactory: dslFactory
                )

        siteJobBuilder.build()
    }

    private void createSelenium() {
        out.println("createSelenium()")
        RmcaSeleniumJobBuilder rmcaSeleniumJobBuilder = new RmcaSeleniumJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_selenium",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                timeoutForJob: timeout,
                generateGUIconfig: this.generateGUIconfig,
                extraEnvironmentVariables: this.extraEnvironmentVariables,
                profilesToBeUsed: SELENIUM_CUCUMBER_PROFILE,
                symlinkWorkspace: this.symlinkWorkspace,
                dslFactory: dslFactory
                )

        Job rmcaSelenium = rmcaSeleniumJobBuilder.build()
        rmcaSelenium.disabled()
    }

    private void createSeleniumGerrit() {
        out.println("createSeleniumGerrit()")
        RmcaGerritSeleniumJobBuilder rmcaGerritSeleniumJobBuilder = new RmcaGerritSeleniumJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_selenium",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                timeoutForJob: timeout,
                generateGUIconfig: this.generateGUIconfig,
                extraEnvironmentVariables: this.extraEnvironmentVariables,
                profilesToBeUsed: SELENIUM_JIVE_PROFILE,
                symlinkWorkspace: this.symlinkWorkspace,
                dslFactory: dslFactory
                )

        rmcaGerritSeleniumJobBuilder.build()
    }

    @Override
    protected void createUnittestGerrit() {
        out.println("createUnittestGerrit()")
        RmcaGerritUnitTestJobBuilder rmcaGerritUnitTestJobBuilder = new RmcaGerritUnitTestJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_unit_test",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                timeoutForJob: timeout,
                generateGUIconfig: this.generateGUIconfig,
                extraEnvironmentVariables: this.extraEnvironmentVariables,
                symlinkWorkspace: this.symlinkWorkspace,
                profilesToBeUsed: 'gui',
                dslFactory: dslFactory
                )

        rmcaGerritUnitTestJobBuilder.build()
    }

    @Override
    protected void createMvnDependencyTest() {
        out.println("createMvnDependencyTest()")
        GerritDependencyTestJobBuilder gerritDependencyTestJobBuilder = new GerritDependencyTestJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_dependency_test",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                timeoutForJob: timeout,
                generateGUIconfig: this.generateGUIconfig,
                extraEnvironmentVariables: this.extraEnvironmentVariables,
                profilesToBeUsed: 'gui',
                dslFactory: dslFactory
                )

        gerritDependencyTestJobBuilder.build()
    }

    protected void createSonarGerrit() {
        out.println("createSonarGerrit()")
        GerritSonarJobBuilder gerritSonarJobBuilder = new RmcaGerritSonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                timeoutForJob: timeout,
                projectName: projectName,
                generateGUIconfig: this.generateGUIconfig,
                extraEnvironmentVariables: this.extraEnvironmentVariables,
                profilesToBeUsed: 'gui',
                dslFactory: dslFactory
                )

        gerritSonarJobBuilder.build()
    }

    @Override
    protected void createReleaseSonarGerrit() {
        out.println("createReleaseSonarGerrit()")

        def releaseProjects = getReleaseBranches()

        if (releaseProjects.size() != 0 && releaseProjects != null) {
            releaseProjects.each {
                def branchName = it
                out.println('[INFO] Create sonar gerrit job for: ' + branchName)

                GerritSonarJobBuilder gerritSonarJobBuilder = new RmcaGerritSonarJobBuilder(
                        gerritUser: gerritUser,
                        gerritServer: gerritServer,
                        jobName: folderName + "/release/" + jobName + "_" + branchName.replace('/', '_') + '_gerrit_sonar',
                        mavenRepositoryPath: mavenRepositoryPath,
                        mavenSettingsFile: mvnSettingFile,
                        gerritName: gerritName,
                        projectName: projectName,
                        timeoutForJob: timeout,
                        generateGUIconfig: this.generateGUIconfig,
                        extraEnvironmentVariables: this.extraEnvironmentVariables,
                        profilesToBeUsed: 'gui',
                        dslFactory: dslFactory,
                        branchName: branchName
                        )

                gerritSonarJobBuilder.build()
            }
        }
    }

    @Override
    protected void createSiteGerrit() {
        out.println("createSiteGerrit()")
        GerritSiteJobBuilder gerritSiteJobBuilder = new GerritSiteJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_site",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                timeoutForJob: timeout,
                generateGUIconfig: this.generateGUIconfig,
                extraEnvironmentVariables: this.extraEnvironmentVariables,
                profilesToBeUsed: 'gui',
                projectName: projectName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory
                )

        gerritSiteJobBuilder.build()
    }

    protected void createDeploy() {
        out.println("createDeploy()")

        overrideJvmOptionsForDebug()

        String releaseGoalDeploy = RELEASE_GOAL + ' -DpublishToJivePortal=true'

        DeployJobBuilder deployJobBuilder = new RmcaDeployJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                releaseGoal: releaseGoalDeploy,
                releaseDryrunGoal: RELEASE_DRYRUN_GOAL,
                jobName: folderName + "/" + jobName + "_deploy",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                timeoutForJob: deployTimeout,
                generateGUIconfig: this.generateGUIconfig,
                extraEnvironmentVariables: this.extraEnvironmentVariables,
                profilesToBeUsed: GUI_PROFILE,
                dslFactory: dslFactory,
                extraMavenParameters: "-DpublishToJivePortal=true"
                )

        deployJobBuilder.build()
    }

    protected void createReleaseDeploy() {
        out.println("createReleaseDeploy()")

        def releaseProjects = getReleaseBranches()

        if (releaseProjects.size() != 0 && releaseProjects != null) {
            releaseProjects.each {
                def branchName = it
                out.println('[INFO] Create release branch for: ' + branchName)
                DeployJobBuilder rmcaReleaseBranchDeployJobBuilder = new RmcaReleaseBranchDeployJobBuilder(
                        gerritUser: gerritUser,
                        gerritServer: gerritServer,
                        releaseGoal: RELEASE_GOAL.replaceAll(GUI_PROFILE, GUI_PROFILE + ',' + SELENIUM_PROFILE_WITHOUT_CUCUMBER),
                        releaseDryrunGoal: RELEASE_DRYRUN_GOAL.replaceAll(GUI_PROFILE, GUI_PROFILE + ',' + SELENIUM_PROFILE_WITHOUT_CUCUMBER),
                        jobName: folderName + "/release/" + jobName + "_" + branchName.replace('/', '_'),
                        mavenRepositoryPath: mavenRepositoryPath,
                        mavenSettingsFile: mvnSettingFile,
                        gerritName: gerritName,
                        projectName: projectName,
                        timeoutForJob: deployTimeout,
                        generateGUIconfig: this.generateGUIconfig,
                        extraEnvironmentVariables: this.extraEnvironmentVariables,
                        profilesToBeUsed: GUI_PROFILE + ',' + SELENIUM_PROFILE_WITHOUT_CUCUMBER,
                        dslFactory: dslFactory
                        )

                rmcaReleaseBranchDeployJobBuilder.buildReleaseBranch(branchName)
            }
        }
    }

    @Override
    protected void createReleaseBranch() {
        CreateBranchJobBuilder createBranchJobBuilder = new CreateBranchJobBuilder(
                workspacePath: workspacePathMesos,
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/release/_" + jobName + "_create_new_release_branch",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                profilesToBeUsed: GUI_PROFILE + ',' + SELENIUM_PROFILE_WITHOUT_CUCUMBER,
                projectName: projectName,
                dslFactory: dslFactory
                )

        createBranchJobBuilder.build()
    }

    protected void createFeatureDeploy() {
        out.println("createFeatureDeploy()")

        def featureProjects = getFeatureBranches()

        if (featureProjects != null && featureProjects.size() != 0) {
            featureProjects.each {
                def branchName = it
                out.println('[INFO] Create feature branch for: ' + branchName)
                DeployJobBuilder featureBranchDeployJobBuilder = new RmcaDeployJobBuilder(
                        gerritUser: gerritUser,
                        gerritServer: gerritServer,
                        releaseGoal: RELEASE_GOAL.replaceAll(GUI_PROFILE, GUI_PROFILE + ',' + SELENIUM_PROFILE_WITHOUT_CUCUMBER),
                        releaseDryrunGoal: RELEASE_DRYRUN_GOAL.replaceAll(GUI_PROFILE, GUI_PROFILE + ',' + SELENIUM_PROFILE_WITHOUT_CUCUMBER),
                        jobName: folderName + "/feature/" + jobName + "_" + branchName.replace('/', '_') + "_deploy",
                        mavenRepositoryPath: mavenRepositoryPath,
                        mavenSettingsFile: mvnSettingFile,
                        gerritName: gerritName,
                        projectName: projectName,
                        timeoutForJob: deployTimeout,
                        generateGUIconfig: this.generateGUIconfig,
                        extraEnvironmentVariables: this.extraEnvironmentVariables,
                        profilesToBeUsed: GUI_PROFILE + ',' + SELENIUM_PROFILE_WITHOUT_CUCUMBER,
                        dslFactory: dslFactory
                )

                featureBranchDeployJobBuilder.buildReleaseBranch(branchName)
            }
        }
    }

    @Override
    protected void createAutoAddReviewer()
    {
        String[] reviewers = null
        String[][] topicPatterns = null
        String[] jobNameSuffix = [""]

        if ( jobName == "configurations" ) {
            reviewers = ['"CHA Testdata Reviewers"', '"RMCA Testdata Reviewers"']
            topicPatterns = [["beta/src/main/resources/usecases/cha/**", "sharp/src/main/resources/usecases/cha/**"],
                                ["beta/src/main/resources/usecases/rmca/**", "sharp/src/main/resources/usecases/rmca/**",
                                    "beta/src/main/resources/baseConfiguration/rmca/**", "sharp/src/main/resources/baseConfiguration/rmca/**"]]
            jobNameSuffix = ["_cha", "_rmca"]
        }
        else if ( jobName == "rmca" ) {
            reviewers = ['"Group RMCA GUI"']
            topicPatterns = [["ui/**"]]
            jobNameSuffix = ["_ui"]
        }

        reviewers.eachWithIndex { reviewer, index ->
            AutoAddReviewerJobBuilder autoAddReviewerJobBuilder = new AutoAddReviewerJobBuilder(
                gerritUser: gerritUser,
                reviewers: [reviewers[index]],
                codeReviewTrigger: codeReviewTrigger,
                gerritTopicPatterns: topicPatterns[index],
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_add_reviewers" + jobNameSuffix[index],
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                dslFactory: dslFactory
                )
            autoAddReviewerJobBuilder.build()
        }
    }

    private overrideJvmOptions() {
        this.extraEnvironmentVariables = [:]
        this.extraEnvironmentVariables.put('JAVA_TOOL_OPTIONS', AbstractJobBuilder.JAVA_TOOL_OPTIONS + ' -DOSGI_PORT_OVERRIDE=\${ALLOCATED_PORT}')
        this.extraEnvironmentVariables.put('MAVEN_OPTS', AbstractJobBuilder.MAVEN_OPTS  + ' -DOSGI_PORT_OVERRIDE=\${ALLOCATED_PORT}')
    }

    /*
    Temporary solution for tracking possible memory leaks.
     */
    private overrideJvmOptionsForDebug() {
        String extraJavaOptions = ' -DOSGI_PORT_OVERRIDE=\${ALLOCATED_PORT} -XX:+HeapDumpOnOutOfMemoryError ' +
                '-XX:HeapDumpPath=/proj/eta-team/tmp/rmca_deploy_dumps -verbose:gc'

        this.extraEnvironmentVariables = [:]
        this.extraEnvironmentVariables.put('JAVA_TOOL_OPTIONS', AbstractJobBuilder.JAVA_TOOL_OPTIONS + extraJavaOptions)
        this.extraEnvironmentVariables.put('MAVEN_OPTS', AbstractJobBuilder.MAVEN_OPTS + extraJavaOptions)
    }

    private void createReleasePrepareJob() {
        out.println("createReleasePrepareJob()")
        String releaseGoalLocal = ""
        if (RELEASE_GOAL.contains("Dgoals=deploy")){
            releaseGoalLocal = RELEASE_GOAL.replace('Dgoals=deploy', 'Dgoals=install')
        }
        else {
            releaseGoalLocal = RELEASE_GOAL + ' -Dgoals=install'
        }

        releaseGoalLocal += ' -DpushChanges=false -DlocalCheckout=true'

        overrideJvmOptions()
        RmcaReleasePrepareJobBuilder rmcaRleasePrepareJobBuilder = new RmcaReleasePrepareJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                releaseGoalLocal: releaseGoalLocal,
                jobName: projectName + "_continuously_dryrun_release",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                timeoutForJob: deployTimeout,
                generateGUIconfig: this.generateGUIconfig,
                extraEnvironmentVariables: this.extraEnvironmentVariables,
                symlinkWorkspace: this.symlinkWorkspace,
                dslFactory: dslFactory,
                )

        rmcaRleasePrepareJobBuilder.build()
    }

    void createBackwardCompatibilityTest() {
        out.println("createBackwardCompatibilityTest()")
        BackwardCompatibilityTestJobBuilder backwardCompatibilityTestBuilder = new BackwardCompatibilityTestJobBuilder(
            gerritUser: gerritUser,
            gerritServer: gerritServer,
            jobName: folderName + "/" + jobName + "_backward_compatibility_test",
            mavenRepositoryPath: mavenRepositoryPath,
            mavenSettingsFile: mvnSettingFile,
            generateGUIconfig: this.generateGUIconfig,
            dslFactory: dslFactory,
            projectName: projectName,
            gerritName: gerritName,
            mailRecipients: "cc:rmca_washingmachine@mailman.lmera.ericsson.se"
            )
        backwardCompatibilityTestBuilder.build()
    }

    void createIntegrationTest() {
        createIntegrationTestCpm()
        createIntegrationTestReleasedCpm()
        createIntegrationTestCha()
        createIntegrationTestReleasedCha()
    }

    void createIntegrationTestCpm() {
        out.println("createIntegrationTest()")
        IntegrationTestJobBuilder integrationTestJobBuilder = new IntegrationTestJobBuilder(
            gerritUser: gerritUser,
            gerritServer: gerritServer,
            jobName: folderName + "/" + jobName + "_integrationtest_cpm",
            mavenRepositoryPath: mavenRepositoryPath,
            mavenSettingsFile: mvnSettingFile,
            generateGUIconfig: this.generateGUIconfig,
            dslFactory: dslFactory,
            projectName: projectName,
            gerritName: gerritName,
            mailRecipients: "cc:anders.t.toverland@ericsson.com, cc:par.engelholm@ericsson.com",
            targetGerrit: "charging/com.ericsson.bss.rm.cpm",
            enabled: false
            )
       integrationTestJobBuilder.build()
    }

    void createIntegrationTestReleasedCpm() {
       IntegrationTestJobBuilder integrationTestJobBuilder = new IntegrationTestJobBuilder(
            gerritUser: gerritUser,
            gerritServer: gerritServer,
            jobName: folderName + "/" + jobName + "_integrationtest_released_cpm",
            mavenRepositoryPath: mavenRepositoryPath,
            mavenSettingsFile: mvnSettingFile,
            generateGUIconfig: this.generateGUIconfig,
            dslFactory: dslFactory,
            projectName: projectName,
            gerritName: gerritName,
            mailRecipients: "cc:anders.t.toverland@ericsson.com, cc:par.engelholm@ericsson.com",
            targetGerrit: "charging/com.ericsson.bss.rm.cpm",
            enabled: false
            )
       integrationTestJobBuilder.build()
    }

    void createIntegrationTestCha() {
       IntegrationTestJobBuilder integrationTestJobBuilder = new IntegrationTestJobBuilder(
            gerritUser: gerritUser,
            gerritServer: gerritServer,
            jobName: folderName + "/" + jobName + "_integrationtest_cha",
            mavenRepositoryPath: mavenRepositoryPath,
            mavenSettingsFile: mvnSettingFile,
            generateGUIconfig: this.generateGUIconfig,
            dslFactory: dslFactory,
            projectName: projectName,
            gerritName: gerritName,
            mailRecipients: "cc:anders.t.toverland@ericsson.com, cc:par.engelholm@ericsson.com",
            targetGerrit: "charging/com.ericsson.bss.rm.charging",
            enabled: false
            )
       integrationTestJobBuilder.build()
    }

    void createIntegrationTestReleasedCha() {
       IntegrationTestJobBuilder integrationTestJobBuilder = new IntegrationTestJobBuilder(
            gerritUser: gerritUser,
            gerritServer: gerritServer,
            jobName: folderName + "/" + jobName + "_integrationtest_released_cha",
            mavenRepositoryPath: mavenRepositoryPath,
            mavenSettingsFile: mvnSettingFile,
            generateGUIconfig: this.generateGUIconfig,
            dslFactory: dslFactory,
            projectName: projectName,
            gerritName: gerritName,
            mailRecipients: "cc:anders.t.toverland@ericsson.com, cc:par.engelholm@ericsson.com",
            targetGerrit: "charging/com.ericsson.bss.rm.charging",
            enabled: false
            )
       integrationTestJobBuilder.build()
    }

    void createVideoDeploy() {
        RmcaVideoDeployJobBuilder rmcaVideoDeployJobBuilder = new RmcaVideoDeployJobBuilder(
                workspacePath: workspacePath,
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_video_deploy",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                timeoutForJob: deployTimeout,
                generateGUIconfig: this.generateGUIconfig,
                dslFactory: dslFactory,
                gerritName: gerritName,
                symlinkWorkspace: this.symlinkWorkspace,
                extraEnvironmentVariables: this.extraEnvironmentVariables,
                profilesToBeUsed: SELENIUM_JIVE_PROFILE
                )

        rmcaVideoDeployJobBuilder.build()
    }

    @Override
    protected List getRepositoriesForCodeFreeze() {
        return getRepositories()
    }

    @Override
    protected String getCodeFreezeApprovers() {
        '''
        Gerrit account ids for user:
        efomatt Mattias Forsman - 1169
        egusfra Gustav Fransson - 1161
        ecanlom Christian Lindblom - 1530
        '''
        return "1169 1161 1530"
    }
}
