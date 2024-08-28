package com.ericsson.bss

import com.ericsson.bss.job.AddRemoveNightlyFullTargethostInstallJobBuilder
import com.ericsson.bss.job.AddRemoveNightlyWashingmachineMsvCilUpgradeJobBuilder
import com.ericsson.bss.job.BuildMultiplePatchsetsJobBuilder
import com.ericsson.bss.job.CilTargethostRollbackJobBuilder
import com.ericsson.bss.job.CreateBranchJobBuilder
import com.ericsson.bss.job.CreateClusterJobBuilder
import com.ericsson.bss.job.CreateReleaseBranchWashingMachineJobBuilder
import com.ericsson.bss.job.DeployJobBuilder
import com.ericsson.bss.job.DslJobBuilder
import com.ericsson.bss.job.EpValidatorJobBuilder
import com.ericsson.bss.job.GerritCodeFreezeJobBuilder
import com.ericsson.bss.job.NpmPerformTestsJobBuilder
import com.ericsson.bss.job.NpmPerformCodeAnalysisJobBuilder
import com.ericsson.bss.job.GerritDependencyTestJobBuilder
import com.ericsson.bss.job.GerritIntegrationTestJobBuilder
import com.ericsson.bss.job.GerritJiveClusterUpgradeJobBuilder
import com.ericsson.bss.job.GerritJiveTestJobBuilder
import com.ericsson.bss.job.GerritPomCheckJobBuilder
import com.ericsson.bss.job.GerritSiteJobBuilder
import com.ericsson.bss.job.GerritSoftwareRecordJobBuilder
import com.ericsson.bss.job.GerritSonarJobBuilder
import com.ericsson.bss.job.IntegrationWashinmachineJobBuilder
import com.ericsson.bss.job.MvnGerritUnitTestJobBuilder
import com.ericsson.bss.job.GitStatsJobBuilder
import com.ericsson.bss.job.NightlyTargethostInstallJobBuilder
import com.ericsson.bss.job.NightlyWashingmachineMsvCilUpgradeJobBuilder
import com.ericsson.bss.job.NpmDeployReleaseFromMasterJobBuilder
import com.ericsson.bss.job.NpmDeploySnapshotJobBuilder
import com.ericsson.bss.job.NpmCreatePatchBranchJobBuilder
import com.ericsson.bss.job.NpmDeployReleaseFromPatchBranchJobBuilder
import com.ericsson.bss.job.NpmPerformCodeAnalysisForReviewJobBuilder
import com.ericsson.bss.job.NpmPerformTestsForReviewJobBuilder
import com.ericsson.bss.job.OvfBuildJobBuilder
import com.ericsson.bss.job.PrepareArm2GaskJobBuilder
import com.ericsson.bss.job.RemoveClusterJobBuilder
import com.ericsson.bss.job.RemoveReleaseBranchWashingMachineJobBuilder
import com.ericsson.bss.job.SetMsvCilJobBuilder
import com.ericsson.bss.job.SiteJobBuilder
import com.ericsson.bss.job.SonarJobBuilder
import com.ericsson.bss.job.TargethostInstallJobBuilder
import com.ericsson.bss.job.TargethostInstallRpmJobBuilder
import com.ericsson.bss.job.TargethostUpgradeJobBuilder
import com.ericsson.bss.job.UmiTestJobBuilder
import com.ericsson.bss.job.UpgradeMsvCilJobBuilder
import com.ericsson.bss.job.washingmachine.WashingMachineReleaseBranchJobBuilder
import com.ericsson.bss.job.washingmachine.WashingMachineReleaseBranchKeepaliveJobBuilder
import com.ericsson.bss.job.washingmachine.WashingMachineReleaseBranchOnOffJobBuilder
import com.ericsson.bss.job.washingmachine.WashingMachinesJobsCreator
import com.ericsson.bss.job.washingmachine.WashingMachineUpgradeJobBuilder
import com.ericsson.bss.job.washingmachine.WashingMachineUpgradeKeepAliveJobBuilder
import com.ericsson.bss.job.washingmachine.WashingMachineUpgradeOnOffJobBuilder
import com.ericsson.bss.util.GerritUtil
import com.ericsson.bss.util.GitUtil
import com.ericsson.bss.util.JobContext
import groovy.xml.MarkupBuilder
import hudson.FilePath
import hudson.util.RemotingDiagnostics
import javaposse.jobdsl.dsl.DslFactory

class Project {
    protected static final String DEFAULT_RELEASE_GOAL = "-Dresume=false release:prepare release:perform"
    protected static final String DEFAULT_DRYRUN_RELEASE_GOAL = "-Dresume=false -DdryRun=true release:prepare"
    protected static final String PUBLISH_TO_JIVE_GOAL = "-DpublishToJivePortal=true"

    protected static final int ALLOCATE_MEMORY_IN_GIGABITE = 1024
    protected static final int ALLOCATED_CPU_IN_CORE = 1

    /** GERRIT_EPK_USER and GERRIT_EPK_SERVER are specific for washingmachines jobs and dsl jobs itself and can be hardcoded.
     * Those static variables shouldn't be used in other places, because different projects can use different users and servers
     * (so it's not possible ex.: to replace all the occurances of 'kascmadm' in all scripts)
     */
    public static final String GERRIT_EPK_USER = 'kascmadm'
    public static final String GERRIT_EPK_SERVER = 'gerrit.epk.ericsson.se'
    public static final String GERRIT_FORGE_SERVER = 'gerritforge.lmera.ericsson.se'
    public static final String GERRIT_CENTRAL_SERVER = 'gerrit.ericsson.se'

    public static final HashMap<String, List> DEFAULT_VALUES_OF_RESOURCE_PROFILES = ['TestSystem':['2', '4096'], 'Default':['', '']]

    private Map cachedExecutionOutput = new HashMap<String, String>()
    private Map cachedRecentlyFetched = new HashMap<FilePath, Boolean>()

    protected Map config = new HashMap()
    protected def out

    protected DslFactory dslFactory
    protected String delimiter
    protected String folderName

    protected String gerritName
    protected String gerritServer
    protected String gerritUser

    protected String integrationTestRepository
    protected String jenkinsURL
    protected String jobName
    protected String mavenRepositoryPath
    protected String mvnSettingFile
    protected String mvnSettingFilePath = AbstractJobBuilder.MAVEN_SETTINGS_PATH
    protected String projectName
    protected String blameMailList
    protected boolean verboseGerritFeedback = true
    protected boolean prepareArm2Gask = false

    protected String releaseGoal = DEFAULT_RELEASE_GOAL
    protected String releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL

    protected String secondIntegrationTestRepository
    protected String workspacePath
    protected String workspacePathMesos
    protected boolean symlinkWorkspace = true
    protected boolean generateSiteJobs = true
    protected List<String> branches = []
    private Map extraEnvironmentVariables = [:]
    protected String extraMavenParameters
    protected List<String> featureBranchesToTestAndAnalyze = []

    // npm-based projects
    public String npmRegistry
    public String targetMachine
    public static final String NPM_PERFORM_TESTS_FOR_REVIEW_JOB_NAME = "perform_tests_for_review"
    public static final String NPM_PERFORM_TESTS_JOB_NAME = "perform_tests"
    public static final String NPM_PERFORM_CODE_ANALYSIS_FOR_REVIEW_JOB_NAME = "perform_code_analysis_for_review"
    public static final String NPM_PERFORM_CODE_ANALYSIS_JOB_NAME = "perform_code_analysis"
    public static final String NPM_DEPLOY_RELEASE_JOB_NAME = "deploy_release"
    public static final String NPM_DEPLOY_SNAPSHOT_JOB_NAME = "deploy_snapshot"
    public static final String NPM_CREATE_PATCH_BRANCH_JOB_NAME = "create_patch_branch"
    public List<String> npmRepositories = []

    protected void init(parent) {
        this.out = parent.out
        this.dslFactory = parent as DslFactory
        JobContext.setDSLFactory(dslFactory)

        this.config.putAll(dslFactory.getBinding().getVariables())
        this.jenkinsURL = config["JENKINS_URL"]

        out.println("Project init(), projectName: " + projectName)

        this.gerritServer = GERRIT_EPK_SERVER
        this.gerritUser = System.getProperty("user.name")
        out.println("gerrituser = " + gerritUser)

        this.workspacePath = getWorkspacePath()
        this.workspacePathMesos = getWorkspacePathMesos()
        this.mavenRepositoryPath = getMavenRepositoryPath()
        this.integrationTestRepository = null
        this.delimiter = '/'
        out.println("Project init done")
    }

    private String getJenkinsEncodedUrl() {
        String encodedUrl = URLEncoder.encode(jenkinsURL, "UTF-8")
        encodedUrl = encodedUrl.replaceAll('%', '_')

        return encodedUrl
    }

    private String getWorkspacePath() {
        return '/local/' + getJenkinsEncodedUrl() + '/\${JOB_NAME}_\${EXECUTOR_NUMBER}'
    }

    private String getWorkspacePathMesos() {
        return 'workspace/\${JOB_NAME}'
    }

    private String getMavenRepositoryPath() {
        return '\${WORKSPACE}/.repository'
    }

    public void create(parent) {
        this.init(parent)
        this.createViews()
        this.createWashingMachinesJobs()

        List repositories = this.getRepositories()
        createGitStatsJob(repositories)
        createBuildMultiplePatchsetJob()
        repositories.each {
            String repository = it
            gerritName = repository
            jobName = getJobName(repository)
            String gitUrl = GitUtil.getGitUrl(gerritServer, gerritName)
            branches = getBranchesList(gitUrl)
            this.createForRepository()
        }
        createDslJob(repositories)
    }

    protected void createTargethostInstallJob(params)
    {
        // arguments list cannot be empty
        assert params != null

        // mandatory arguments
        assert params['versionLocation']
        assert params['defaultTapasJobPath']
        List versionLocation = params.versionLocation
        String defaultTapasJobPath = params.defaultTapasJobPath

        // optional arguments with default values
        String installNodeName = (params.installNodeName == null) ? "" : params.installNodeName
        String suite = params.suite ?: "suites/targethost_install.xml"
        String suiteTwoHosts = params.suiteTwoHosts ?: "suites/targethost_install_two_hosts.xml"
        HashMap<String, List> valuesOfResourceProfiles =
                params.valuesOfResourceProfiles ?: DEFAULT_VALUES_OF_RESOURCE_PROFILES
        List<String> resourceProfiles = valuesOfResourceProfiles.keySet().toList()
        String resourceProfilesDescription = params.resourceProfilesDescription ?:
                ('Specifies how much hardware resources (CPU and RAM) ' +
                    'the targethosts should be deployed with. Normally the "TestSystem" profile should be used.')
        List targethostDescription = params.targethostDescription ?:
                ["Targethost which will be updated"]
        boolean useCil = (params.useCil == null) ? true : params.useCil
        List installType = params.installType ?: []
        Class TargethostInstallJobBuilderClass = params['targethostInstallJobBuilderClass'] ?: TargethostInstallJobBuilder
        boolean useJiveTests = (params.useJiveTests == null) ? false : params.useJiveTests
        String jiveMetaData = (params.jiveMetaData == null) ? "" : params.jiveMetaData
        boolean useSeleniumTests = (params.useSeleniumTests == null) ? false : params.useSeleniumTests
        String seleniumMetaData = (params.seleniumMetaData == null) ? "" : params.seleniumMetaData
        boolean useDvFile = (params.useDvFile == null) ? false : params.useDvFile
        boolean useResourceProfile = (params.useResourceProfile == null) ? true : params.useResourceProfile
        boolean useTestData = (params.useTestData == null) ? false : params.useTestData
        String testdataVersionLocation = (params.testdataVersionLocation == null) ? "" : params.testdataVersionLocation
        int nrOfNetworks = (params.nrOfNetworks== null) ? 1 : params.nrOfNetworks
        boolean useAppGroup = (params.useAppGroup == null) ? false : params.useAppGroup
        List ovfPacName = (params.ovfPacName == null) ? [] : params.ovfPacName
        boolean useMultipleCils = (params.useMultipleCils == null) ? false : params.useMultipleCils
        boolean useMultipleTargethosts = (params.useMultipleTargethosts == null) ? true : params.useMultipleTargethosts

        out.println("createTargethostInstallJob()")
        TargethostInstallJobBuilder builder = TargethostInstallJobBuilderClass.newInstance(
                out: out,
                installNodeName: installNodeName,
                versionLocation: versionLocation,
                defaultTapasJobPath: defaultTapasJobPath,
                suite: suite,
                suiteTwoHosts: suiteTwoHosts,
                resourceProfiles: resourceProfiles,
                resourceProfilesDescription: resourceProfilesDescription,
                valuesOfResourceProfiles: valuesOfResourceProfiles,
                targethostDescription: targethostDescription,
                useCil: useCil,
                timeoutForJob: 120,
                symlinkWorkspace: true,
                workspacePath: workspacePath,
                dslFactory: dslFactory,
                jobName: projectName + '_targethost_install',
                projectName: projectName,
                gerritServer: '',
                useJiveTests: useJiveTests,
                jiveMetaData: jiveMetaData,
                useSeleniumTests: useSeleniumTests,
                seleniumMetaData: seleniumMetaData,
                useDvFile: useDvFile,
                useResourceProfile: useResourceProfile,
                useTestData: useTestData,
                testdataVersionLocation: testdataVersionLocation,
                nrOfNetworks: nrOfNetworks,
                installType: installType,
                useAppGroup: useAppGroup,
                ovfPacName: ovfPacName,
                useMultipleCils: useMultipleCils,
                useMultipleTargethosts: useMultipleTargethosts
                )
        builder.build()
    }

    protected void createTargethostUpgradeJob(params)
    {
        // arguments list cannot be empty
        assert params != null

        // mandatory arguments
        assert params['versionLocation']
        assert params['defaultTapasJobPath']
        String versionLocation = params.versionLocation
        String defaultTapasJobPath = params.defaultTapasJobPath

        // optional arguments with default values
        Class TargethostUpgradeJobBuilderClass = params['targethostUpgradeJobBuilderClass'] ?: TargethostUpgradeJobBuilder
        String suite = params.suite ?: "suites/targethost_upgrade.xml"
        boolean useCil = (params.useCil == null) ? true : params.useCil
        int nrOfNetworks = params.nrOfNetworks ?: 1

        out.println("createTargethostUpgradeJob()")
        TargethostUpgradeJobBuilder builder = TargethostUpgradeJobBuilderClass.newInstance(
                out: out,
                versionLocation: versionLocation,
                defaultTapasJobPath: defaultTapasJobPath,
                suite: suite,
                useCil: useCil,
                timeoutForJob: 180,
                symlinkWorkspace: true,
                workspacePath: workspacePath,
                dslFactory: dslFactory,
                jobName: projectName + '_targethost_upgrade',
                projectName: projectName,
                nrOfNetworks: nrOfNetworks,
                gerritServer: ''
                )
        builder.build()
    }

    protected void createWashingMachineUpgradeJob(params) {
        out.println("createWashingMachineUpgradeJob()")

        assert params['defaultTapasJobPath']
        assert params['uniqueConfigIdentifier']
        assert params['variantArtifacts']
        assert params['targetHosts']
        String defaultTapasJobPath = params.defaultTapasJobPath
        String uniqueConfigIdentifier = params.uniqueConfigIdentifier
        ArrayList variantArtifacts = params.variantArtifacts
        ArrayList targetHosts = params.targetHosts

        String suite = params.suite ?: "suites/washingmachine_upgrade.xml"

        Class WashingMachineUpgradeJobBuilderClass = params['washingMachineUpgradeJobBuilderClass'] ?: WashingMachineUpgradeJobBuilder

        WashingMachineUpgradeJobBuilder upgradeBuilder = WashingMachineUpgradeJobBuilderClass.newInstance(
            out: out,
            gerritUser: gerritUser,
            gerritServer: gerritServer,
            projectName: projectName,
            dslFactory: dslFactory,
            timeoutForJob: 120,
            symlinkWorkspace: false,
            workspacePath: workspacePath,
            defaultTapasJobPath: defaultTapasJobPath,
            uniqueConfigIdentifier: uniqueConfigIdentifier,
            suite: suite,
            targetHosts: targetHosts,
            variantArtifacts: variantArtifacts
        )
        upgradeBuilder.build()
    }

    protected void createEpValidatorJob(params)
    {
        // arguments list cannot be empty
        assert params != null

        // mandatory arguments
        assert params['versionLocation']
        assert params['defaultTapasJobPath']
        String versionLocation = params.versionLocation
        String defaultTapasJobPath = params.defaultTapasJobPath

        // optional arguments with default values
        boolean useCil = (params.useCil == null) ? true : params.useCil
        boolean useDvFile = (params.useDvFile == null) ? false : params.useDvFile
        boolean useJiveTests = (params.useJiveTests == null) ? false : params.useJiveTests
        boolean useSeleniumTests = (params.useSeleniumTests == null) ? false : params.useSeleniumTests
        boolean useResourceProfile = (params.useResourceProfile == null) ? true : params.useResourceProfile
        boolean useTwoTargethosts = (params.useTwoTargethosts == null) ? false : params.useTwoTargethosts

        Class EpValidatorJobBuilderClass = params['epValidatorJobBuilderClass'] ?: EpValidatorJobBuilder

        String jiveMetaData = (params.jiveMetaData == null) ? "" : params.jiveMetaData
        String seleniumMetaData = (params.seleniumMetaData == null) ? "" : params.seleniumMetaData
        String suite = params.suite ?: "suites/ep_validator.xml"
        String targethostDescription = params.targethostDescription ?: "Targethost which will be updated"
        String targethostDescription2 = params.targethostDescription2 ?: "Targethost2 which will be updated"
        String versionLocation2 = params.versionLocation2 ?: ""

        HashMap<String, List> valuesOfResourceProfiles = params.valuesOfResourceProfiles ?: DEFAULT_VALUES_OF_RESOURCE_PROFILES
        List<String> resourceProfiles = valuesOfResourceProfiles.keySet().toList()

        out.println("createEpValidatorJob()")
        EpValidatorJobBuilder builder = EpValidatorJobBuilderClass.newInstance(
                out: out,
                versionLocation: versionLocation,
                versionLocation2: versionLocation2,
                suite: suite,
                defaultTapasJobPath: defaultTapasJobPath,
                useTwoTargethosts: useTwoTargethosts,
                resourceProfiles: resourceProfiles,
                targethostDescription: targethostDescription,
                targethostDescription2: targethostDescription2,
                useCil: useCil,
                timeoutForJob: 180,
                symlinkWorkspace: true,
                workspacePath: workspacePath,
                dslFactory: dslFactory,
                jobName: projectName + '_ep_verification',
                projectName: projectName,
                gerritServer: '',
                useJiveTests: useJiveTests,
                jiveMetaData: jiveMetaData,
                useSeleniumTests: useSeleniumTests,
                seleniumMetaData: seleniumMetaData,
                useDvFile: useDvFile,
                useResourceProfile: useResourceProfile
        )
        builder.build()
    }

    protected void createWashingMachineKeepAliveJob(params) {
        out.println("createWashingKeepAliveJob()")

        assert params['suffix']
        String suffix = params.suffix

        String schema = params.schema ?: 'H/30 * * * *'
        ArrayList blockingJobs = params.blockingJobs ?: [projectName + '_washingmachine' + suffix]
        String fileName = params.fileName ?: AbstractJobBuilder.PATH_TO_JOB_CONFIG + projectName + '_washingmachine' + suffix + '_params.properties'
        Class WashingMachineUpgradeKeepAliveJobBuilderClass = params['washingMachineKeepAliveJobBuilderClass'] ?: WashingMachineUpgradeKeepAliveJobBuilder

        WashingMachineUpgradeKeepAliveJobBuilder keepAliveBuilder = WashingMachineUpgradeKeepAliveJobBuilderClass.newInstance(
            gerritUser   : gerritUser,
            gerritServer : gerritServer,
            dslFactory   : dslFactory,
            projectName  : projectName,
            out          : out,
            schema       : schema,
            blockingJobs : blockingJobs,
            fileName     : fileName,
            suffix       : suffix
        )
        keepAliveBuilder.build()
    }

    protected void createWashingMachineOnOffJob(params) {
        out.println("createWashingOnOffJob()")

        assert params['suffix']
        assert params['recipient']
        String suffix = params.suffix
        String recipient = params.recipient

        ArrayList variantArtifacts = params.variantArtifacts ?: []

        Class WashingMachineUpgradeOnOffJobBuilderClass = params['washingMachineOnOffJobBuilderClass'] ?: WashingMachineUpgradeOnOffJobBuilder

        WashingMachineUpgradeOnOffJobBuilder OnOffBuilder = WashingMachineUpgradeOnOffJobBuilderClass.newInstance(
            workspacePath   : workspacePath,
            gerritUser      : gerritUser,
            gerritServer    : gerritServer,
            dslFactory      : dslFactory,
            projectName     : projectName,
            out             : out,
            suffix          : suffix,
            recipient       : recipient,
            variantArtifacts: variantArtifacts
        )
        OnOffBuilder.build()
    }

    protected void createInstallRpmJob(String defaultTapasJobPath, String suite = "suites/targethost_install_rpm.xml", String suiteFile = "targethost_install_rpm_\${TARGETHOST}.xml", String tpgVariant = "") {
        out.println("createInstallRpmJob()")
        TargethostInstallRpmJobBuilder builder = new TargethostInstallRpmJobBuilder(
                out: out,
                workspacePath: workspacePath,
                suite: suite,
                suiteFile: suiteFile,
                dslFactory: dslFactory,
                jobName: projectName + '_targethost_rpm_install',
                projectName: projectName,
                gerritServer: '',
                defaultTapasJobPath: defaultTapasJobPath,
                variant: tpgVariant
                )
        builder.build()
    }

    protected void createUmiTestJob(params) {

        // arguments list cannot be empty
        assert params != null

        // mandatory arguments
        assert params['defaultTapasJobPath']
        assert params['suite']
        assert params['suiteFile']
        String defaultTapasJobPath = params.defaultTapasJobPath
        String suite = params.suite
        String suiteFile = params.suiteFile

        boolean useTwoTargethosts = (params.useTwoTargethosts == null) ? false : params.useTwoTargethosts
        String extraDescription = (params.extraDescription == null) ? "" : params.extraDescription
        boolean withInstallNodePool = (params.withInstallNodePool == null) ? false : params.withInstallNodePool
        String installNodePool = (params.installNodePool == null) ? "" : params.installNodePool
        boolean useCil = (params.useCil == null) ? true : params.useCil
        Class umiTestJobBuilderClass = params['umiTestJobBuilderClass'] ?: UmiTestJobBuilder
        boolean runXvfb = (params.runXvfb == null) ? false : params.runXvfb
        int timeoutForJob = (params.timeoutForJob == null) ? 180 : params.timeoutForJob

        out.println("createUmiTestJob()")
        UmiTestJobBuilder builder = umiTestJobBuilderClass.newInstance(
                out: out,
                workspacePath: workspacePath,
                suite: suite,
                suiteFile: suiteFile,
                dslFactory: dslFactory,
                jobName: projectName + '_umi_test',
                projectName: projectName,
                gerritServer: '',
                defaultTapasJobPath: defaultTapasJobPath,
                useTwoTargethosts: useTwoTargethosts,
                withInstallNodePool: withInstallNodePool,
                installNodePool: installNodePool,
                runXvfb: runXvfb,
                extraDescription: extraDescription,
                symlinkWorkspace: true,
                useCil: useCil,
                timeoutForJob: timeoutForJob
                )
        builder.build()
    }

    protected void createOvfBuildJob(String defaultTapasJobPath, String suite, String suiteFile,
                                         List<String> variants = [],
                                         HashMap<String,String> variantsToOvfPacnames = [:]) {
        out.println("createOvfBuildJob()")
        OvfBuildJobBuilder builder = new OvfBuildJobBuilder(
            out: out,
            workspacePath: workspacePath,
            suite: suite,
            suiteFile: suiteFile,
            dslFactory: dslFactory,
            jobName: projectName + '_ovf_build',
            projectName: projectName,
            gerritServer: '',
            defaultTapasJobPath: defaultTapasJobPath,
            runXvfb: true,
            variants: variants,
            variantsToOvfPacnames: variantsToOvfPacnames,
            symlinkWorkspace: true
            )
        builder.build()
    }

    protected void createWashingMachinesJobs() {
        WashingMachinesJobsCreator creator = new WashingMachinesJobsCreator(
                out: out,
                workspacePath: workspacePath,
                gerritUser: GERRIT_EPK_USER,
                gerritServer: GERRIT_EPK_SERVER,
                dslFactory: dslFactory,
                projectName: projectName
                )
        creator.create()
    }

    protected void createDslJob(List repositories) {
        out.println("createDslJobs()")
        DslJobBuilder createDslJobBuilder = new DslJobBuilder(
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

    private void createGitStatsJob(List repositories ) {
        out.println("createGitStatsJob()")
        GitStatsJobBuilder dslGitStatsJobBuilder = new GitStatsJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                projectName: projectName,
                jobName: projectName + '_gitstats',
                repositories: repositories,
                dslFactory: dslFactory
                )

        dslGitStatsJobBuilder.build()
    }

    protected void createBuildMultiplePatchsetJob() {
        out.println("createMultiPatchsetTriggerJob()")

        BuildMultiplePatchsetsJobBuilder buildMultiplePatchsetJobBuilder = new BuildMultiplePatchsetsJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                projectName: projectName,
                jobName: projectName + '_build_multiple_patchset',
                dslFactory: dslFactory
                )

        buildMultiplePatchsetJobBuilder.build()
    }

    protected void createCilTargethostRollbackJob() {
        out.println("createCilTargethostRollbackJob()")
        CilTargethostRollbackJobBuilder targethostResetCilJobBuilder = new CilTargethostRollbackJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                projectName: projectName,
                jobName: projectName + '_cil_targethost_rollback',
                dslFactory: dslFactory,
                out: out,
                runXvfb: false
        )

        targethostResetCilJobBuilder.build()
    }

    protected void createViews() {
        String linkToGitStatpage = dslFactory.getAt('JENKINS_URL') +
                'job/' + projectName +
                '_gitstats/lastSuccessfulBuild/artifact/gitstats_out/index.html'

        String componentsDescription = "<p>Here you get an overview of the repositories that belongs to " + projectName + ".</p>\n" +
                "<p>\n" +
                "  The deploy job in the right section shows the status of the latest build on the master branch." +
                " In the folder on the left side, you will find all jobs that are related to that specific repository.<br />\n" +
                "  You can click on each job to get some more detailed information what it does.<br />\n" +
                "</p>\n" +
                "<h4>Some example of jobs that exist for a common repository</h4>\n" +
                "<ul>\n" +
                "  <li>deploy - build and publish the source</li>\n" +
                "  <li>sonar - code analyze</li>\n" +
                "  <li>site - build and publish documentation</li>\n" +
                "  <li>gerrit - jobs that are triggered in the review phase</li>\n" +
                "</ul>\n" +
                AbstractJobBuilder.BSSF_MAVEN_CI_DESCRIPTION +
                "Git statistics for " + projectName + " related repositories can be found <a href=" + linkToGitStatpage + ">here</a>."

        dslFactory.nestedView(projectName.capitalize()) {
            configure { project ->
                project / defaultView << 'Components'
            }
            views{
                sectionedView('Components') {
                    description(componentsDescription)
                    sections {
                        listView {
                            name('')
                            width('THIRD')
                            alignment('LEFT')
                            jobs {
                                regex('(?!.*?' + projectName + '/.*/.*)' + projectName + '/.*')
                            }
                            columns {
                                status()
                                name()
                            }
                        }
                    }
                    sections {
                        listView {
                            name('')
                            width('TWO_THIRDS')
                            alignment('RIGHT')
                            jobs { regex('(?!.*?feature)' + projectName + '/.*deploy') }
                            columns {
                                status()
                                weather()
                                name()
                                lastDuration()
                                lastBuildConsole()
                            }
                        }
                    }
                }

                listView('All') {
                    statusFilter(StatusFilter.ENABLED)
                    jobs { regex( '^(?!' + projectName + '.*washingmachine).*(' + projectName + '.*)$' ) }
                    columns {
                        status()
                        weather()
                        name()
                        lastDuration()
                        lastBuildConsole()
                        lastSuccess()
                        lastFailure()
                        configureProject()
                        buildButton()
                    }
                }
                listView('Deploy') {
                    description(DeployJobBuilder.JOB_DESCRIPTION)
                    statusFilter(StatusFilter.ENABLED)
                    jobs { regex('(?!.*?feature)' + projectName + '/.*_deploy') }
                    columns {
                        status()
                        weather()
                        name()
                        lastDuration()
                        lastBuildConsole()
                        lastSuccess()
                        lastFailure()
                        configureProject()
                        buildButton()
                    }
                    recurse(true)
                }
                listView('Sonar') {
                    description(SonarJobBuilder.JOB_DESCRIPTION)
                    statusFilter(StatusFilter.ENABLED)
                    jobs {
                        regex('(?!.*?gerrit)' + projectName + '/.*_sonar')
                    }
                    columns {
                        status()
                        weather()
                        name()
                        lastDuration()
                        lastBuildConsole()
                        lastSuccess()
                        lastFailure()
                        configureProject()
                        buildButton()
                    }
                    recurse(true)
                }
                listView('Washingmachines') {
                    statusFilter(StatusFilter.ENABLED)
                    jobs { regex(projectName + '.*washingmachine.*') }
                    columns {
                        status()
                        weather()
                        name()
                        lastDuration()
                        lastBuildConsole()
                        lastSuccess()
                        lastFailure()
                        configureProject()
                        buildButton()
                    }
                    recurse(true)
                }

                if (generateSiteJobs) {
                    listView('Site') {
                        description(SiteJobBuilder.JOB_DESCRIPTION)
                        statusFilter(StatusFilter.ENABLED)
                        jobs { regex('(?!.*?gerrit)' + projectName + '/.*_site') }
                        columns {
                            status()
                            weather()
                            name()
                            lastDuration()
                            lastBuildConsole()
                            lastSuccess()
                            lastFailure()
                            configureProject()
                            buildButton()
                        }
                        recurse(true)
                    }
                }
            }
        }
    }

    protected List getRepositories() {
        //TODO: Should be forced with interface instead, so that does that are
        // inheriting from Project are forced to implement method.
        return null
    }

    protected List getRepositoriesForGerritJive() {
        //TODO: Should be forced with interface instead, so that does that are
        // inheriting from Project are forced to implement method.
        return null
    }

    protected boolean shouldRepositoryHaveGuiConfig(String repo) {
        return repo in getGuiRepositories()
    }

    /**
     * Method to be overridden in child classes, defining which repos
     * that should have GUI config included.
     * @return List of repositories that should have GUI config generated
     */
    protected List getGuiRepositories() {
        List<String> repositories = []
        return repositories
    }

    protected String getJobName(String repository) {
        return repository.substring(repository.indexOf(delimiter) + 1, repository.length())
    }

    public boolean runProject(String projectName) {
        return projectName == this.projectName
    }

    protected List getNpmRepositories() {
        return npmRepositories
    }

    protected boolean isNpmRepository() {
        gerritName in getNpmRepositories()
    }

    protected void createForRepository() {
        out.println("projectName: " + projectName + ", jobname: " + jobName +
                ", gerritName: " + gerritName)

        if ( isNpmRepository() ) {
            createNpmFolders()
            createNpmJobs()
            return
        }

        createFolders()
        createAutoAddReviewer()
        if (getRepositoriesForGerritJive().any { it.equals(gerritName) }) {
            createJiveTestGerrit()
        }

        if (isNpmRepository()) {
            createNpmJobs()
            return
        }

        if (generateSiteJobs) {
            createSite()
        }
        createPomAnalysisGerrit()
        createSonar()
        createDeploy()
        if (!GitUtil.isLocatedInGitolite(gerritName)) {
            createSonarGerrit()
            createUnittestGerrit()
            createMvnDependencyTest()
            createIntegrationTestGerrit()
            if (generateSiteJobs) {
                createSiteGerrit()
            }
        }
        createReleaseDeploy()
        createReleaseBranch()
        createReleaseSonar()
        createReleaseSonarGerrit()
    }

    protected void createFolders() {
        out.println("createFolders()")
        folderName = projectName + "/" + jobName
        dslFactory.folder(projectName) { }
        dslFactory.folder(folderName) { }
        dslFactory.folder(folderName + "/release") { }
        jobName = jobName.replace('.', '_')
    }

    protected void createNpmFolders() {
        out.println("createNpmFolders()")
        folderName = projectName + "/" + jobName
        dslFactory.folder(projectName) { }
        dslFactory.folder(folderName) { }
        branches.each{
            dslFactory.folder(folderName + '/' + it) { }
        }
        jobName = jobName.replace('.', '_')
    }

    protected void createSite(int timeout = getDefaultSiteJobTimeout()) {
        out.println("createSite()")
        SiteJobBuilder siteJobBuilder = new SiteJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_site",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                dslFactory: dslFactory,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                timeoutForJob: timeout
                )

        siteJobBuilder.build()
    }

    protected void createSonar(int timeout = getDefaultSonarJobTimeout()) {
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
                timeoutForJob: timeout
                )

        sonarJobBuilder.build()
    }

    protected void createSonarGerrit(int timeout = getDefaultSonarJobTimeout()) {
        out.println("createSonarGerrit()")
        GerritSonarJobBuilder gerritSonarJobBuilder = new GerritSonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                projectName: projectName,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                timeoutForJob: timeout
                )

        gerritSonarJobBuilder.build()
    }

    protected void createDeploy(int timeout = getDefaultDeployJobTimeout()) {
        out.println("createDeploy()")
        DeployJobBuilder deployJobBuilder = new DeployJobBuilder(
                workspacePath: workspacePathMesos,
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
                prepareArm2Gask: prepareArm2Gask,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                timeoutForJob: timeout
                )

        deployJobBuilder.build()
    }

    protected void createUnittestGerrit(int timeout = getDefaultUnitTestJobTimeout()) {
        out.println("createUnittestGerrit()")
        MvnGerritUnitTestJobBuilder gerritUnitTestJobBuilder = new MvnGerritUnitTestJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_unit_test",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                timeoutForJob: timeout
                )

        gerritUnitTestJobBuilder.build()
    }

    protected void createSiteGerrit(int timeout = getDefaultSiteJobTimeout()) {
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
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                timeoutForJob: timeout
                )

        gerritSiteJobBuilder.build()
    }

    protected void createCreateClusterJob(boolean useTwoTargethosts = false,
                                          List tpgSpecificMachines = [],
                                          boolean useCil = true) {
        out.println("createCreateClusterJob()")
        CreateClusterJobBuilder createClusterJobBuilder = new CreateClusterJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: projectName + '_create_cluster',
                projectName: projectName,
                dslFactory: dslFactory,
                useTwoTargethosts: useTwoTargethosts,
                tpgSpecificMachines: tpgSpecificMachines,
                useCil: useCil
                )

        createClusterJobBuilder.build()
    }

    protected void createRemoveClusterJob() {
        out.println("createRemoveClusterJob()")
        RemoveClusterJobBuilder removeClusterJobBuilder = new RemoveClusterJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: projectName + '_remove_cluster',
                projectName: projectName,
                dslFactory: dslFactory
                )

        removeClusterJobBuilder.build()
    }

    protected void createReleaseBranchWashingMachineJobBuilder(boolean useTwoTargethosts = false, boolean useGitBranch = false) {
        out.println("CreateReleaseBranchWashingMachineJobBuilder()")
        CreateReleaseBranchWashingMachineJobBuilder createReleaseBranchWashingMachineJobBuilder = new CreateReleaseBranchWashingMachineJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: projectName + '_create_washingmachine_releasebranch',
                projectName: projectName,
                dslFactory: dslFactory,
                runXvfb: false,
                useTwoTargethosts: useTwoTargethosts,
                useGitBranch: useGitBranch

        )

        createReleaseBranchWashingMachineJobBuilder.build()
    }

    protected void removeReleaseBranchWashingMachineJobBuilder(boolean useRpmWm = false) {
        out.println("RemoveReleaseBranchWashingMachineJobBuilder()")
        RemoveReleaseBranchWashingMachineJobBuilder removeReleaseBranchWashingMachineJobBuilder = new RemoveReleaseBranchWashingMachineJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: projectName + '_remove_washingmachine_releasebranch',
                projectName: projectName,
                dslFactory: dslFactory,
                runXvfb: false,
                useRpmWm: useRpmWm
        )

        removeReleaseBranchWashingMachineJobBuilder.build()
    }

    protected void createWashingMachineReleaseBranch(params) {
        out.println("CreateWashingMachineReleaseBranch()")

        // arguments list cannot be empty
        assert params != null

        // mandatory arguments
        assert params['mailingList']
        assert params['defaultTapasJobPath']
        String mailingList = params.mailingList
        String defaultTapasJobPath = params.defaultTapasJobPath

        // optional arguments with default values
        Class washingMachineReleaseBranchJobBuilderClass = params['washingMachineReleaseBranchJobBuilderClass'] ?: WashingMachineReleaseBranchJobBuilder
        String suite = params.suite ?: "suites/washingmachine_branch.xml"
        String rpmSuite = params.rpmSuite ?: "suites/washingmachine_branch_rpm.xml"
        boolean useRpmWm = params.useRpmWm ?: false
        boolean useDlb = params.useDlb ?: false
        String wmBuildTimer = useRpmWm ? "H 1 * * *" : "H/30 * * * *"
        int timeoutForJob = params.timeoutForJob ?: 240
        int timeoutForJobRpm = params.timeoutForJobRpm ?: 60
        boolean useGitBranch = (params.useGitBranch == null) ? false : params.useGitBranch

        File file = new File(AbstractJobBuilder.PATH_TO_JOB_CONFIG, projectName + '_release_branch_washingmachine')
        if ( !file.exists() ) {
            file.createNewFile()
            StringWriter sw = new StringWriter()
            MarkupBuilder xml = new MarkupBuilder(sw)
            xml.clusters()
            FileWriter fw = new FileWriter(file)
            fw.write(sw.toString())
            fw.close()
            return
        }
        XmlParser parser = new XmlParser()
        Node branches = parser.parse(file)

        branches.children().each {
            String propertiesFile =  AbstractJobBuilder.PATH_TO_JOB_CONFIG + projectName + '_washingmachine_releasebranch_' +
                    it.@releasebranchname + '_params.properties'

            // Create washingmachine jobs
            washingMachineReleaseBranchJobBuilder(washingMachineReleaseBranchJobBuilderClass: washingMachineReleaseBranchJobBuilderClass,
                                                  releaseBranch: it,
                                                  defaultTapasJobPath: defaultTapasJobPath,
                                                  suite: suite,
                                                  useRpmWm: useRpmWm,
                                                  timeoutForJob: timeoutForJob,
                                                  useDlb: useDlb,
                                                  useGitBranch: useGitBranch)

            washingMachineReleaseBranchOnOffJobBuilder(it, mailingList)
            washingMachineReleaseBranchKeepaliveJobBuilder(releaseBranch: it,
                                                           fileName: propertiesFile,
                                                           buildTimer: wmBuildTimer,
                                                           useRpmWm: useRpmWm)
            if (useRpmWm) {
                // Create washingmachine rpm jobs
                washingMachineReleaseBranchJobBuilder(washingMachineReleaseBranchJobBuilderClass: washingMachineReleaseBranchJobBuilderClass,
                                                      releaseBranch: it,
                                                      defaultTapasJobPath: defaultTapasJobPath,
                                                      suite: rpmSuite,
                                                      useRpmWm: useRpmWm,
                                                      isRpm: true,
                                                      timeoutForJob: timeoutForJobRpm,
                                                      useDlb: useDlb)
                washingMachineReleaseBranchOnOffJobBuilder(it, mailingList, true)
                washingMachineReleaseBranchKeepaliveJobBuilder(releaseBranch: it,
                                                               fileName: propertiesFile,
                                                               buildTimer: 'H/10 4-23 * * *',
                                                               useRpmWm: useRpmWm,
                                                               isRpm: true,
                                                               useGitBranch: true)
            }
        }
    }

    protected void washingMachineReleaseBranchJobBuilder(params) {
        out.println("WashingMachineReleaseBranchJobBuilder()")

        // arguments list cannot be empty
        assert params != null

        // mandatory arguments
        assert params['releaseBranch']
        assert params['defaultTapasJobPath']
        assert params['washingMachineReleaseBranchJobBuilderClass']
        Class washingMachineReleaseBranchJobBuilderClass = params.washingMachineReleaseBranchJobBuilderClass

        // optional arguments with default values
        String suite = params.suite ?: "suites/washingmachine_branch.xml"
        boolean useRpmWm = params.useRpmWm ?: false
        boolean isRpm = params.isRpm ?: false
        boolean useDlb = params.useDlb ?: false
        boolean useGitBranch = params.useGitBranch == null ? false : params.useGitBranch
        int timeoutForJob = params.timeoutForJob ?: 240

        WashingMachineReleaseBranchJobBuilder builder = washingMachineReleaseBranchJobBuilderClass.newInstance(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                projectName: projectName,
                dslFactory: dslFactory,
                workspacePath: workspacePath,
                defaultTapasJobPath: params.defaultTapasJobPath,
                suite: suite,
                out: out,
                releaseBranch : params.releaseBranch,
                useRpmWm: useRpmWm,
                isRpm: isRpm,
                timeoutForJob: timeoutForJob,
                useGitBranch: useGitBranch,
                useDlb: useDlb
        )
        builder.build()
    }

    protected void washingMachineReleaseBranchOnOffJobBuilder(Object releaseBranch, String recipient, boolean isRpm = false) {
        out.println("WashingMachineReleaseBranchOnoffBuilder()")
        WashingMachineReleaseBranchOnOffJobBuilder washingMachineReleaseBranchOnOffJobBuilder = new WashingMachineReleaseBranchOnOffJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                projectName: projectName,
                dslFactory: dslFactory,
                workspacePath: workspacePath,
                out: out,
                releaseBranch : releaseBranch,
                recipient: recipient,
                runXvfb: false,
                isRpm: isRpm
        )
        washingMachineReleaseBranchOnOffJobBuilder.build()
    }

    protected void washingMachineReleaseBranchKeepaliveJobBuilder(params) {
        out.println("washingMachineReleaseBranchKeepaliveJobBuilder")

        assert params != null
        assert params['releaseBranch']
        assert params['fileName']

        Object releaseBranch = params.releaseBranch
        String fileName = params.fileName
        String buildTimer = params.buildTimer ?: 'H 1 * * *'
        boolean useRpmWm = params.useRpmWm ?: false
        boolean isRpm = params.isRpm ?: false
        Class washingMachineReleaseBranchOnOffJobBuilderClass = (params.washingMachineReleaseBranchOnOffJobBuilderClass ?:
                                                                 WashingMachineReleaseBranchKeepaliveJobBuilder)

        WashingMachineReleaseBranchKeepaliveJobBuilder builder = washingMachineReleaseBranchOnOffJobBuilderClass.newInstance(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                projectName: projectName,
                dslFactory: dslFactory,
                workspacePath: workspacePath,
                out: out,
                releaseBranch : releaseBranch,
                fileName: fileName,
                runXvfb: false,
                useRpmWm: useRpmWm,
                isRpm: isRpm,
                buildTimer: buildTimer
        )
        builder.build()
    }

    protected void createMvnDependencyTest(int timeout = getDefaultMvnDependencyJobTimeout()) {
        out.println("createMvnDependencyTest()")
        GerritDependencyTestJobBuilder gerritDependencyTestJobBuilder = new GerritDependencyTestJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_dependency_test",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory,
                timeoutForJob: timeout
                )

        gerritDependencyTestJobBuilder.build()
    }

    protected void createSoftwareRecordJob() {
        out.println("createSoftwareRecordJob()")
        GerritSoftwareRecordJobBuilder gerritSoftwareRecordJobBuilder = new GerritSoftwareRecordJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: projectName + '_software_record',
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                dslFactory: dslFactory
                )

        gerritSoftwareRecordJobBuilder.build()
    }

    private void createPrepareArm2GaskJob() {
        out.println("createPrepareArm2GaskJob()")
        PrepareArm2GaskJobBuilder arm2GaskJobBuilder = new PrepareArm2GaskJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: projectName + '_prepare_arm2gask',
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                dslFactory: dslFactory
                )

        arm2GaskJobBuilder.build()
    }

    protected List getIgnoredRepositoriesForIntegrationTest() {
        return [
            'bundle',
            'devenv',
            'integrationtest',
            'test_integration',
            'jive',
            'integration',
            'top',
            'umi'
        ]
    }

    protected void createIntegrationTestGerrit(int timeout = getDefaultIntegrationTestJobTimeout()) {
        out.println("createIntegrationTestGerrit()")
        boolean ignoreRepository = jobName in getIgnoredRepositoriesForIntegrationTest()

        if (!ignoreRepository && integrationTestRepository != null) {
            GerritIntegrationTestJobBuilder gerritIntegrationTestJobBuilder = new GerritIntegrationTestJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_gerrit_integration_test",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    projectName: projectName,
                    integrationTestRepository: integrationTestRepository,
                    verboseGerritFeedback: verboseGerritFeedback,
                    dslFactory: dslFactory,
                    timeoutForJob: timeout,
                    generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                    extraMavenParameters: extraMavenParameters
                    )

            gerritIntegrationTestJobBuilder.build()
        }
    }

    protected void createReleaseDeploy(int timeout = getDefaultReleaseJobTimeout()) {
        out.println("createReleaseDeploy()")

        List releaseProjects = getReleaseBranches()

        if (releaseProjects.size() != 0 && releaseProjects != null) {
            releaseProjects.each {
                String branchName = it
                out.println('[INFO] Create release branch for: ' + branchName)
                DeployJobBuilder releaseBranchDeployJobBuilder = new DeployJobBuilder(
                        workspacePath: workspacePathMesos,
                        gerritUser: gerritUser,
                        gerritServer: gerritServer,
                        releaseGoal: releaseGoal,
                        releaseDryrunGoal: releaseDryrunGoal,
                        jobName: folderName + "/release/" + jobName + "_" + branchName.replace('/', '_'),
                        mavenRepositoryPath: mavenRepositoryPath,
                        mavenSettingsFile: mvnSettingFile,
                        gerritName: gerritName,
                        projectName: projectName,
                        dslFactory: dslFactory,
                        generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                        timeoutForJob: timeout
                        )

                releaseBranchDeployJobBuilder.buildReleaseBranch(branchName)
            }
        }
    }

    protected void createReleaseSonar(int timeout = getDefaultReleaseJobTimeout()) {
        out.println("createReleaseSonar()")

        List releaseProjects = getReleaseBranches()

        if (releaseProjects.size() != 0 && releaseProjects != null) {
            releaseProjects.each {
                String branchName = it
                out.println('[INFO] Create sonar job for: ' + branchName)
                SonarJobBuilder sonarJobBuilder = new SonarJobBuilder(
                        gerritUser: gerritUser,
                        gerritServer: gerritServer,
                        jobName: folderName + "/release/" + jobName + "_" + branchName.replace('/', '_') + '_sonar',
                        mavenRepositoryPath: mavenRepositoryPath,
                        mavenSettingsFile: mvnSettingFile,
                        gerritName: gerritName,
                        dslFactory: dslFactory,
                        branchName: branchName,
                        generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                        timeoutForJob: timeout
                        )

                sonarJobBuilder.build()
            }
        }
    }

    protected void createReleaseSonarGerrit(int timeout = getDefaultReleaseJobTimeout()) {
        out.println("createReleaseSonarGerrit()")

        List releaseProjects = getReleaseBranches()

        if (releaseProjects.size() != 0 && releaseProjects != null) {
            releaseProjects.each {
                String branchName = it
                out.println('[INFO] Create sonar gerrit job for: ' + branchName)

                GerritSonarJobBuilder gerritSonarJobBuilder = new GerritSonarJobBuilder(
                        gerritUser: gerritUser,
                        gerritServer: gerritServer,
                        jobName: folderName + "/release/" + jobName + "_" + branchName.replace('/', '_') + '_gerrit_sonar',
                        mavenRepositoryPath: mavenRepositoryPath,
                        mavenSettingsFile: mvnSettingFile,
                        gerritName: gerritName,
                        projectName: projectName,
                        verboseGerritFeedback: verboseGerritFeedback,
                        dslFactory: dslFactory,
                        branchName: branchName,
                        generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                        timeoutForJob: timeout
                        )

                gerritSonarJobBuilder.build()
            }
        }
    }

    protected void createAutoAddReviewer() {
        //Override in sub-project to add auto add reviewer job
    }

    protected void createPomAnalysisGerrit(int timeout = getDefaultPomAnalysisJobTimeout()) {
        out.println("createPomAnalysisGerrit()")

        if (gerritName in getPomCheckRepositoryList()){
            GerritPomCheckJobBuilder gerritpomcheckbuilder = new GerritPomCheckJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_gerrit_check_properties",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    verboseGerritFeedback: verboseGerritFeedback,
                    dslFactory: dslFactory,
                    timeoutForJob: timeout
                    )

            gerritpomcheckbuilder.build()
        }
    }

    protected List getPomCheckRepositoryList(){
        List<String> pomCheckRepoList = []
        return pomCheckRepoList
    }

    protected void createReleaseBranch(int timeout = getDefaultReleaseJobTimeout()) {
        CreateBranchJobBuilder createBranchJobBuilder = new CreateBranchJobBuilder(
                workspacePath: workspacePathMesos,
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/release/_" + jobName + "_create_new_release_branch",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                dslFactory: dslFactory,
                timeoutForJob: timeout
                )

        createBranchJobBuilder.build()
    }

    protected List getReleaseBranches() {
        List releaseBranches = []
        String gitUrl = GitUtil.getGitUrl(gerritServer, gerritName)

        if (hasReleaseBranches(gitUrl)) {
            FilePath localRepositoryFolder = getLocalRepositoryFolder(gerritName)

            if (gitRepositoryExist(localRepositoryFolder)) {
                gitFetchCachedRepository(localRepositoryFolder)
            }
            else {
                gitCloneRepository(localRepositoryFolder, gitUrl)
            }

            if (gitRepositoryExist(localRepositoryFolder)) {
                releaseBranches = getValidReleaseBranches(localRepositoryFolder)
            }
            else {
                out.println '[ERROR] Not able to find git repository: ' + localRepositoryFolder
            }
        }

        return releaseBranches
    }

    protected void createFeatureDeploy() {
        out.println("createFeatureDeploy()")

        List featureProjects = getFeatureBranches()

        if (featureProjects != null && featureProjects.size() != 0) {
            featureProjects.each {
                String branchName = it
                out.println('[INFO] Create feature branch for: ' + branchName)
                DeployJobBuilder featureBranchDeployJobBuilder = new DeployJobBuilder(
                        workspacePath: workspacePathMesos,
                        gerritUser: gerritUser,
                        gerritServer: gerritServer,
                        releaseGoal: releaseGoal,
                        releaseDryrunGoal: releaseDryrunGoal,
                        jobName: folderName + "/feature/" + jobName + "_" + branchName.replace('/', '_') + "_deploy",
                        mavenRepositoryPath: mavenRepositoryPath,
                        mavenSettingsFile: mvnSettingFile,
                        gerritName: gerritName,
                        projectName: projectName,
                        dslFactory: dslFactory,
                        timeoutForJob: getDefaultJobTimeout()
                        )

                featureBranchDeployJobBuilder.buildReleaseBranch(branchName)
            }
        }
    }

    protected void createFeatureSonar(int timeout = getDefaultFeatureJobTimeout()) {
        out.println("createFeatureSonar()")

        List featureProjects = getFeatureBranches()

        if (featureProjects != null && featureProjects.size() != 0) {
            featureProjects.each {
                String branchName = it

                if (branchName in featureBranchesToTestAndAnalyze) {
                    out.println('[INFO] Create sonar job for: ' + branchName)
                    SonarJobBuilder sonarJobBuilder = new SonarJobBuilder(
                            gerritUser: gerritUser,
                            gerritServer: gerritServer,
                            jobName: folderName + "/feature/" + jobName + "_" + branchName.replace('/', '_') + '_sonar',
                            mavenRepositoryPath: mavenRepositoryPath,
                            mavenSettingsFile: mvnSettingFile,
                            gerritName: gerritName,
                            dslFactory: dslFactory,
                            branchName: branchName,
                            generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                            timeoutForJob: timeout
                    )

                    sonarJobBuilder.build()
                }
            }
        }
    }

    protected void createFeatureSonarGerrit(int timeout = getDefaultReleaseJobTimeout()) {
        out.println("createFeatureSonarGerrit()")

        List featureProjects = getFeatureBranches()

        if (featureProjects != null && featureProjects.size() != 0) {
            featureProjects.each {
                String branchName = it
                out.println('[INFO] Create sonar gerrit job for: ' + branchName)

                if (branchName in featureBranchesToTestAndAnalyze) {
                    GerritSonarJobBuilder gerritSonarJobBuilder = new GerritSonarJobBuilder(
                            gerritUser: gerritUser,
                            gerritServer: gerritServer,
                            jobName: folderName + "/feature/" + jobName + "_" + branchName.replace('/', '_') + '_gerrit_sonar',
                            mavenRepositoryPath: mavenRepositoryPath,
                            mavenSettingsFile: mvnSettingFile,
                            gerritName: gerritName,
                            projectName: projectName,
                            verboseGerritFeedback: verboseGerritFeedback,
                            dslFactory: dslFactory,
                            branchName: branchName,
                            generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                            timeoutForJob: timeout
                    )

                    gerritSonarJobBuilder.build()
                }
            }
        }
    }

    protected List getFeatureBranches() {
        List featureBranches = []
        String gitUrl = GitUtil.getGitUrl(gerritServer, gerritName)

        if (hasFeatureBranches(gitUrl)) {
            FilePath localRepositoryFolder = getLocalRepositoryFolder(gerritName)

            if (gitRepositoryExist(localRepositoryFolder)) {
                gitFetchRepository(localRepositoryFolder)
            }
            else {
                gitCloneRepository(localRepositoryFolder, gitUrl)
            }

            if (gitRepositoryExist(localRepositoryFolder)) {
                featureBranches = getValidFeatureBranches(localRepositoryFolder)
            }
            else {
                out.println '[ERROR] Not able to find git repository: ' + localRepositoryFolder
            }
        }

        return featureBranches
    }

    private FilePath getLocalRepositoryFolder(String gitRepositoryName) {
        def build = Thread.currentThread().executable
        def workspace = build.workspace.toString()
        def virtualChannel = build.workspace.channel

        FilePath filePath = new FilePath(virtualChannel, workspace + '/.tmpRepository/' + gitRepositoryName)

        out.println('[INFO] Local repository folder: ' + filePath)

        return filePath
    }

    protected createJiveTestGerrit(String script = "scripts/gerrit_deploy_to_karaf_then_jive.sh", int timeout = 45) {
        out.println("createJiveTestGerrit()")
        new GerritJiveTestJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_jive_test",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                symlinkWorkspace: symlinkWorkspace,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory,
                gerritDeployToKarafScript:script,
                timeoutForJob:timeout
                ).build()
    }

    private boolean hasReleaseBranches(String gitUrl) {
        return hasBranches(gitUrl, 'refs/heads/release/')
    }

    private boolean hasFeatureBranches(String gitUrl) {
        return hasBranches(gitUrl, 'refs/heads/feature/')
    }

    private boolean hasBranches(String gitUrl, String branchName) {
        String getRemoteReferencesCommand = '/opt/local/dev_tools/git/latest/bin/git ls-remote --heads ' + gitUrl

        String featureBranches = executeCachedCommand(getRemoteReferencesCommand)
        if (featureBranches != null) {
            return featureBranches.contains(branchName)
        }

        return false
    }

    private static boolean gitRepositoryExist(FilePath localRepositoryFolder) {
        FilePath repoFile = new FilePath(localRepositoryFolder, 'config')
        return repoFile.exists()
    }

    private void gitFetchCachedRepository(FilePath localRepositoryFolder) {
        if (cachedRecentlyFetched.getOrDefault(localRepositoryFolder, false)) {
            out.println("Skipping git fetch of " + localRepositoryFolder.toURI() + " as it has recently been fetched.")
            return
        }
        gitFetchRepository(localRepositoryFolder)
    }

    private void gitFetchRepository(FilePath localRepositoryFolder) {
        String configWhatShouldBeFetch = "/opt/local/dev_tools/git/latest/bin/git config remote.origin.fetch refs/heads/*:refs/heads/*"
        executeCommand(configWhatShouldBeFetch, localRepositoryFolder)
        String fetchRepositoryCommand = '/opt/local/dev_tools/git/latest/bin/git fetch'
        executeCommand(fetchRepositoryCommand, localRepositoryFolder)
        cachedRecentlyFetched[localRepositoryFolder] = true
    }

    private void gitCloneRepository(FilePath localRepositoryFolder, String gitUrl) {
        localRepositoryFolder.deleteRecursive()
        localRepositoryFolder.mkdirs()
        String cloneRepositoryCommand = '/opt/local/dev_tools/git/latest/bin/git clone --reference ' +
                GitUtil.getCloneReference() + ' --bare ' + gitUrl + ' .'
        executeCommand(cloneRepositoryCommand, localRepositoryFolder)
        cachedRecentlyFetched[localRepositoryFolder] = true
    }

    private List getValidReleaseBranches(FilePath localRepositoryFolder) {
        List<String> releaseBranches = new ArrayList<String>()
        String commandDelimiter = ','

        List<String> releaseBranchesMetaInfo = getReleaseBranchesMetaInfo(commandDelimiter, localRepositoryFolder)

        for (releaseBranchMetaInfo in releaseBranchesMetaInfo) {
            if (!releaseBranchMetaInfo.isEmpty()) {
                String[] metaInfo = releaseBranchMetaInfo.split(commandDelimiter)

                Date branchDate = parseDate(metaInfo)
                if (isValidBranch(branchDate)) {
                    String branchName = parseBranchName(metaInfo)
                    releaseBranches.add(branchName)
                }
            }
        }

        return releaseBranches
    }

    private List getReleaseBranchesMetaInfo(String commandDelimiter, FilePath localRepositoryFolder) {
        List<String> releaseBranchesMetaInfo
        String branchesCommand = '/opt/local/dev_tools/git/latest/bin/git for-each-ref --format=\'%(committerdate:short)' + commandDelimiter +
                '%(refname)\' refs/heads/release/ refs/remotes/origin/release/'
        String returnValue = executeCommand(branchesCommand, localRepositoryFolder)
        releaseBranchesMetaInfo = returnValue.replaceAll('\'', '').split("\n")

        return releaseBranchesMetaInfo
    }

    private List getValidFeatureBranches(FilePath localRepositoryFolder) {
        List<String> featureBranches = new ArrayList<String>()
        String commandDelimiter = ','

        List<String> featureBranchesMetaInfo = getFeatureBranchesMetaInfo(commandDelimiter, localRepositoryFolder)

        for (featureBranchMetaInfo in featureBranchesMetaInfo) {
            if (!featureBranchMetaInfo.isEmpty()) {
                String[] metaInfo = featureBranchMetaInfo.split(commandDelimiter)

                Date branchDate = parseDate(metaInfo)
                if (isValidBranch(branchDate)) {
                    String branchName = parseBranchName(metaInfo)
                    featureBranches.add(branchName)
                }
            }
        }

        return featureBranches
    }

    private List getFeatureBranchesMetaInfo(String commandDelimiter, FilePath localRepositoryFolder) {
        List<String> featureBranchesMetaInfo
        String branchesCommand = '/opt/local/dev_tools/git/latest/bin/git for-each-ref --format=\'%(committerdate:short)' + commandDelimiter +
                '%(refname)\' refs/heads/feature/ refs/remotes/origin/feature/'
        String returnValue = executeCommand(branchesCommand, localRepositoryFolder)
        featureBranchesMetaInfo = returnValue.replaceAll('\'', '').split("\n")

        return featureBranchesMetaInfo
    }

    private boolean isValidBranch(Date branchDate) {
        boolean isValid = false
        Date currentDate = new Date()

        use(groovy.time.TimeCategory) {
            def duration = currentDate - branchDate

            if (duration.days < 100) {
                isValid = true
            }
        }

        return isValid
    }

    private Date parseDate(String[] info) {
        String branchDateText = info[0]
        Date branchDate = new Date().parse("yyyy-MM-dd", branchDateText)
        return branchDate
    }

    private String parseBranchName(String[] info) {
        String referenceName = info[1]
        String branchName

        if (referenceName.contains('refs/heads/')) {
            branchName = info[1].replaceAll('refs/heads/', '')
        }
        else if (referenceName.contains('refs/remotes/origin/')) {
            branchName = info[1].replaceAll('refs/remotes/origin/', '')
        }
        else {
            out.println('[ERROR] not able to parse branch name: ' + referenceName)
        }
        return branchName
    }

    protected String executeCommand(String commandToRun) {
        int maxTimeBeforeTimeout = 90000

        String outputCommand = '[INFO] Executing ' + commandToRun

        long start = System.currentTimeMillis()
        out.println(outputCommand)
        Process proc = commandToRun.execute()
        proc.out.close()  // Ensure that we do not hang waiting on interaction from stdin.
        StringBuilder output = new StringBuilder()
        Thread consumer = proc.consumeProcessOutputStream(output)  // Ensure that we do not hang on full stdout buffer.

        proc.waitForOrKill(maxTimeBeforeTimeout)
        consumer.join(maxTimeBeforeTimeout)

        long now = System.currentTimeMillis()

        if (proc.exitValue() != 0) {
            long duration = now - start
            if (duration >= maxTimeBeforeTimeout) {
                outputCommand = '[ERROR] Timeout ' + duration + 'ms, Max ' +
                        maxTimeBeforeTimeout + 'ms, Not able to run ' +
                        commandToRun
            }
            else{
                outputCommand = '[ERROR] not able to run ' + commandToRun
            }

            //TODO: Print the exception as well to console.
            out.println(outputCommand)
            throw new IOException(outputCommand)
        }

        return output.toString()
    }

    protected String executeCachedCommand(String commandToRun) {
        String output = cachedExecutionOutput.getOrDefault(commandToRun, null)
        if (output != null) {
            out.println("Using " + output.length().toString() + " chars of cached output data from " + commandToRun)
            return output
        }

        output = executeCommand(commandToRun)

        if (output != null && output.trim().length() > 0) {
            cachedExecutionOutput[commandToRun] = output
        }
        return output
    }

    protected String executeCommand(String commandToRun, FilePath workingDirectory) {
        int maxTimeBeforeTimeout = 300000

        out.println('[INFO] Executing ' + commandToRun + ' in ' + workingDirectory)
        String remoteCommand = 'def proc = "' + commandToRun + '".execute(null, new File("' + workingDirectory + '"));' +
                'proc.out.close();' +
                'def output = new java.lang.StringBuilder();' +
                'def consumer = proc.consumeProcessOutputStream(output);' +
                'proc.waitForOrKill(' + maxTimeBeforeTimeout + ');' +
                'consumer.join(' + maxTimeBeforeTimeout + ');' +
                'return output.toString()'

        String resultOutput = RemotingDiagnostics.executeGroovy(remoteCommand, workingDirectory.getChannel())

        return resultOutput.replaceAll('Result: ', '')
    }

    protected String getGerritforgeProjects() {
        String output = ""

        for (int i = 1; i <= 6; i++) {
            try {
                output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))
                break
            } catch (IOException e) {
                if (i < 6) {
                    def sleepTime = (long)((2**i * (1 + Math.random())) * 1000)
                    out.println("Retrying in " + sleepTime + "ms..")
                    sleep(sleepTime)
                    continue
                } else {
                    throw e
                }
            }
        }
        return output
    }

    protected void createUpgradeMsvCil(String jenkins) {
        UpgradeMsvCilJobBuilder upgradeMsvCilJobBuilder = new UpgradeMsvCilJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: projectName + "_upgrade_msv_cil",
                dslFactory: dslFactory,
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                projectName: projectName,
                jenkins: jenkins,
                type: 'team_machine'
                )
        upgradeMsvCilJobBuilder.build()
    }

    protected void createUpgradeGerritJiveMsvCil(String jenkins) {
        UpgradeMsvCilJobBuilder upgradeGerritJiveMsvCil = new UpgradeMsvCilJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: projectName + "_upgrade_gerrit_jive_msv_cil",
                dslFactory: dslFactory,
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                projectName: projectName,
                jenkins: jenkins,
                type: 'gerrit_jive'
                )
        upgradeGerritJiveMsvCil.build()
    }

    protected void createAddRemoveNightlyFullTargethostInstall(params) {

        boolean onlyTargethostInstallation = params?.onlyTargethostInstallation == null ? false : params.onlyTargethostInstallation
        boolean useTwoTargethosts = params?.useTwoTargethosts == null ? false : params.useTwoTargethosts
        boolean useMsvMachine = params?.useMsvMachine == null ? true : params.useMsvMachine
        boolean useCilMachine = params?.useCilMachine == null ? true : params.useCilMachine
        HashMap<String, List> valuesOfResourceProfiles =
                params.valuesOfResourceProfiles ?: DEFAULT_VALUES_OF_RESOURCE_PROFILES
        List<String> resourceProfiles = valuesOfResourceProfiles.keySet().toList()

        Class addRemoveNightlyFullTargethostInstallJobBuilderClass = (params?.addRemoveNightlyFullTargethostInstallJobBuilderClass
            ?: AddRemoveNightlyFullTargethostInstallJobBuilder)

        out.println("createAddRemoveNightlyFullTargethostInstall()")
        AddRemoveNightlyFullTargethostInstallJobBuilder builder = addRemoveNightlyFullTargethostInstallJobBuilderClass.newInstance(
            out: out,
            workspacePath: workspacePath,
            dslFactory: dslFactory,
            jobName: projectName + '_add_remove_nightly_full_targethost_install',
            jenkinsURL: jenkinsURL,
            projectName: projectName,
            gerritServer: gerritServer,
            runXvfb: false,
            useTwoTargethosts: useTwoTargethosts,
            onlyTargethostInstallation: onlyTargethostInstallation,
            useMsvMachine: useMsvMachine,
            useCilMachine: useCilMachine,
            resourceProfiles: resourceProfiles
            )
        builder.build()
    }

    protected void createNightlyFullTargethostInstall(params) {

        assert params != null
        assert params['targethostInstallParameters'] != null

        int cronTrigger = params?.cronTrigger ?: 0
        String msvResourceProfile = params?.msvResourceProfile ?: "TestSystem"
        String cilResourceProfile = params?.cilResourceProfile ?: "TestSystem"
        Class nightlyTargethostInstallJobClass = params?.nightlyTargethostInstallJobClass ?: NightlyTargethostInstallJobBuilder
        LinkedHashMap<String, String> targethostInstallParameters = params.targethostInstallParameters

        out.println("createNightlyFullTargethostInstall()")
        NightlyTargethostInstallJobBuilder builder = nightlyTargethostInstallJobClass.newInstance(
            out: out,
            workspacePath: workspacePath,
            dslFactory: dslFactory,
            jobName: projectName + '_nightly_targethost_install',
            jenkinsURL: jenkinsURL,
            projectName: projectName,
            gerritServer: gerritServer,
            runXvfb: false,
            cronTrigger: cronTrigger,
            targethostInstallParameters: targethostInstallParameters,
            msvResourceProfile: msvResourceProfile,
            cilResourceProfile: cilResourceProfile
        )
        builder.build()

    }

    protected void createSetMsvCilVersion() {
        out.println("createSetMsvCilVersion")
        SetMsvCilJobBuilder builder = new SetMsvCilJobBuilder(
            out: out,
            workspacePath: workspacePath,
            dslFactory: dslFactory,
            jobName: projectName + '_set_msv_cil_version',
            jenkinsURL: jenkinsURL,
            projectName: projectName,
            gerritServer: gerritServer,
            runXvfb: false,
        )
        builder.build()
    }

    protected void createGerritJiveClusterUpgradeJob(params) {

        Class gerritJiveClusterUpgradeJob = GerritJiveClusterUpgradeJobBuilder
        String msvProfile = 'Washingmachine'
        String cilProfile = 'Washingmachine'
        String targethostProfile = 'Washingmachine'

        if (params) {
            if (params.gerritJiveClusterUpgradeJobBuilderClass) {
                gerritJiveClusterUpgradeJob = params.gerritJiveClusterUpgradeJobBuilderClass
            }

            msvProfile = (params?.msvProfile == null) ? msvProfile : params.msvProfile
            cilProfile = (params?.cilProfile == null) ? cilProfile : params.cilProfile
            targethostProfile = (params?.targethostProfile == null) ? targethostProfile : params.targethostProfile
        }

        out.println("createGerritJiveClusterUpgradeJob")
        GerritJiveClusterUpgradeJobBuilder builder = gerritJiveClusterUpgradeJob.newInstance(
            out: out,
            workspacePath: workspacePath,
            dslFactory: dslFactory,
            jobName: projectName + '_gerrit_jive_cluster_upgrade',
            gerritServer: "",
            projectName: projectName,
            msvProfile: msvProfile,
            cilProfile: cilProfile,
            targethostProfile: targethostProfile
        )
        builder.build()
    }

    protected int getDefaultJobTimeout() {
        return AbstractJobBuilder.DEFAULT_JOB_TIMEOUT
    }

    protected int getDefaultDeployJobTimeout() {
        return getDefaultJobTimeout()
    }

    protected int getDefaultSonarJobTimeout() {
        return getDefaultJobTimeout()
    }

    protected int getDefaultUnitTestJobTimeout() {
        return getDefaultJobTimeout()
    }

    protected int getDefaultSiteJobTimeout() {
        return getDefaultJobTimeout()
    }

    protected int getDefaultMvnDependencyJobTimeout() {
        return getDefaultJobTimeout()
    }

    protected int getDefaultIntegrationTestJobTimeout() {
        return getDefaultJobTimeout()
    }

    protected int getDefaultReleaseJobTimeout() {
        return getDefaultJobTimeout()
    }

    protected int getDefaultFeatureJobTimeout() {
        return getDefaultJobTimeout()
    }

    protected int getDefaultPomAnalysisJobTimeout() {
        return getDefaultJobTimeout()
    }

    protected void createAddRemoveNightlyWashingmachineMsvCilUpgrade(boolean useTwoTargethosts = false) {
        out.println("createAddRemoveNightlyWashingmachineMsvCilUpgrade()")
        AddRemoveNightlyWashingmachineMsvCilUpgradeJobBuilder builder = new AddRemoveNightlyWashingmachineMsvCilUpgradeJobBuilder(
            out: out,
            workspacePath: workspacePath,
            dslFactory: dslFactory,
            jobName: projectName + '_add_remove_nightly_washingmachine_msv_cil_upgrade',
            jenkinsURL: jenkinsURL,
            projectName: projectName,
            gerritServer: gerritServer,
            runXvfb: false,
            useTwoTargethosts: useTwoTargethosts
        )
        builder.build()
    }

    protected void createNightlyWashingmachineMsvCilUpgradeJob(params) {

        String msvResourceProfile = params?.msvResourceProfile ?: "Washingmachine"
        String cilResourceProfile = params?.cilResourceProfile ?: "Washingmachine"
        Class nightlyWashingmachineMsvCilUpgradeJobClass = (params?.nightlyWashingmachineMsvCilUpgradeJobClass ?:
                                                            NightlyWashingmachineMsvCilUpgradeJobBuilder)

        out.println("createNightlyWashingmachineMsvCilUpgradeJob()")
        NightlyWashingmachineMsvCilUpgradeJobBuilder builder = nightlyWashingmachineMsvCilUpgradeJobClass.newInstance(
            out: out,
            workspacePath: workspacePath,
            dslFactory: dslFactory,
            jobName: projectName + '_nightly_washingmachine_msv_cil_upgrade',
            jenkinsURL: jenkinsURL,
            projectName: projectName,
            gerritServer: gerritServer,
            runXvfb: false,
            msvResourceProfile: msvResourceProfile,
            cilResourceProfile: cilResourceProfile
        )
        builder.build()
    }

    protected void createNpmPerformTestsForReviewJob(params) {
        out.println("createNpmPerformTestsForReviewJob()")

        // arguments list cannot be empty
        assert params != null

        // mandatory arguments
        assert params['branchName']
        String branchName = params.branchName

        NpmPerformTestsForReviewJobBuilder performTestsForReviewJobBuilder = new NpmPerformTestsForReviewJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + branchName + "/" + NPM_PERFORM_TESTS_FOR_REVIEW_JOB_NAME,
                gerritName: gerritName,
                branchName: branchName,
                projectName: projectName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory,
                npmRegistry: npmRegistry,
                timeoutForJob: getDefaultJobTimeout(),
                targetMachine: targetMachine,
                out: out
        )

        performTestsForReviewJobBuilder.build()
    }

    protected void createNpmPerformTestsJob(params) {
        out.println("createNpmPerformTestsJob()")

        // arguments list cannot be empty
        assert params != null

        // mandatory arguments
        assert params['branchName']
        String branchName = params.branchName

        NpmPerformTestsJobBuilder performTestsJobBuilder = new NpmPerformTestsJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + branchName + "/" + NPM_PERFORM_TESTS_JOB_NAME,
                gerritName: gerritName,
                branchName: branchName,
                dslFactory: dslFactory,
                npmRegistry: npmRegistry,
                timeoutForJob: getDefaultJobTimeout(),
                targetMachine: targetMachine,
                out: out
        )

        performTestsJobBuilder.build()
    }

    protected void createNpmPerformCodeAnalysisJob(params) {
        out.println("createNpmPerformCodeAnalysisJob()")

        // arguments list cannot be empty
        assert params != null

        // mandatory arguments
        assert params['branchName']
        String branchName = params.branchName

        new NpmPerformCodeAnalysisJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + branchName + "/" +
                        NPM_PERFORM_CODE_ANALYSIS_JOB_NAME,
                branchName: branchName,
                gerritName: gerritName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory
        ).build()
    }

    protected void createNpmPerformCodeAnalysisForReviewJob(params) {
        out.println("createNpmPerformCodeAnalysisForReviewJob")

        // arguments list cannot be empty
        assert params != null

        // mandatory arguments
        assert params['branchName']
        String branchName = params.branchName

        new NpmPerformCodeAnalysisForReviewJobBuilder(
                out: out,
                workspacePath: workspacePath,
                dslFactory: dslFactory,
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + branchName + "/" +
                        NPM_PERFORM_CODE_ANALYSIS_FOR_REVIEW_JOB_NAME,
                branchName: branchName,
                timeoutForJob: getDefaultJobTimeout(),
                gerritName: gerritName

        ).build()
    }

    protected void createNpmDeployReleaseFromMasterJob(params) {
        out.println("createNpmDeployReleaseFromMasterJob()")

        // arguments list cannot be empty
        assert params != null

        // mandatory arguments
        assert params['branchName']
        assert params['snapshotTag']
        assert params['releaseTag']
        String branchName = params.branchName
        String snapshotTag = params.snapshotTag
        String releaseTag = params.releaseTag

        // optional arguments with default values
        Class NpmDeployReleaseFromMasterJobBuilderClass = params['NpmDeployReleaseFromMasterJobBuilderClass'] ?: NpmDeployReleaseFromMasterJobBuilder

        NpmDeployReleaseFromMasterJobBuilder builder = NpmDeployReleaseFromMasterJobBuilderClass.newInstance(
                out: out,
                workspacePath: workspacePath,
                dslFactory: dslFactory,
                jobName: folderName + "/" + branchName + "/" + NPM_DEPLOY_RELEASE_JOB_NAME,
                projectName: projectName,
                gerritServer: gerritServer,
                gerritUser: gerritUser,
                gerritName: gerritName,
                timeoutForJob: getDefaultJobTimeout(),
                snapshotDeployJobName: NPM_DEPLOY_SNAPSHOT_JOB_NAME,
                npmRegistry: npmRegistry,
                branchName: branchName,
                targetMachine: targetMachine,
                snapshotTag: snapshotTag,
                releaseTag: releaseTag,
                testJobName: NPM_PERFORM_TESTS_JOB_NAME
        )

        builder.build()
    }

    protected void createNpmDeployReleaseFromPatchBranchJob(params) {
        out.println("createNpmDeployReleaseFromPatchBranchJob()")

        // arguments list cannot be empty
        assert params != null

        // mandatory arguments
        assert params['branchName']
        assert params['snapshotTag']
        assert params['releaseTag']
        String branchName = params.branchName
        String snapshotTag = params.snapshotTag
        String releaseTag = params.releaseTag

        // optional arguments with default values
        Class NpmDeployReleaseFromPatchBranchJobBuilderClass = params['NpmDeployReleaseFromPatchBranchJobBuilderClass'] ?: NpmDeployReleaseFromPatchBranchJobBuilder

        NpmDeployReleaseFromPatchBranchJobBuilder builder = NpmDeployReleaseFromPatchBranchJobBuilderClass.newInstance(
                out: out,
                workspacePath: workspacePath,
                dslFactory: dslFactory,
                jobName: folderName + "/" + branchName + "/" + NPM_DEPLOY_RELEASE_JOB_NAME,
                projectName: projectName,
                gerritServer: gerritServer,
                gerritUser: gerritUser,
                gerritName: gerritName,
                timeoutForJob: getDefaultJobTimeout(),
                snapshotDeployJobName: NPM_DEPLOY_SNAPSHOT_JOB_NAME,
                npmRegistry: npmRegistry,
                branchName: branchName,
                targetMachine: targetMachine,
                snapshotTag: snapshotTag,
                releaseTag: releaseTag,
                testJobName: NPM_PERFORM_TESTS_JOB_NAME
        )

        builder.build()
    }

    protected void createNpmDeploySnapshotJob(params) {
        out.println("createNpmDeploySnapshotJob()")

        // arguments list cannot be empty
        assert params != null

        // mandatory arguments
        assert params['branchName']
        assert params['snapshotTag']
        String branchName = params.branchName
        String snapshotTag = params.snapshotTag

        // optional arguments with default values
        Class NpmDeploySnapshotJobBuilderClass = params['NpmDeploySnapshotJobBuilderClass'] ?: NpmDeploySnapshotJobBuilder

        NpmDeploySnapshotJobBuilder builder = NpmDeploySnapshotJobBuilderClass.newInstance(
                out: out,
                workspacePath: workspacePath,
                dslFactory: dslFactory,
                jobName: folderName + "/" + branchName + "/" + NPM_DEPLOY_SNAPSHOT_JOB_NAME,
                projectName: projectName,
                gerritServer: gerritServer,
                gerritUser: gerritUser,
                gerritName: gerritName,
                timeoutForJob: getDefaultJobTimeout(),
                npmRegistry: npmRegistry,
                branchName: branchName,
                targetMachine: targetMachine,
                snapshotTag: snapshotTag,
                testJobName: NPM_PERFORM_TESTS_JOB_NAME
        )

        builder.build()
    }

    protected void createNpmCreatePatchBranchJob() {
        out.println("createNpmCreatePatchBranchJob()")

        NpmCreatePatchBranchJobBuilder createPatchBranchJobBuilder = new NpmCreatePatchBranchJobBuilder(
                workspacePath: workspacePathMesos,
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + NPM_CREATE_PATCH_BRANCH_JOB_NAME,
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                dslFactory: dslFactory,
                extraEnvironmentVariables: extraEnvironmentVariables,
                timeoutForJob: getDefaultJobTimeout(),
                out: out,
                targetMachine: targetMachine
        )

        createPatchBranchJobBuilder.build()
    }

    protected void createNpmJobs() {
        String snapshotTag = "SNAPSHOT"
        String releaseTag = "latest"

        createNpmCreatePatchBranchJob()

        branches.each {
            createNpmPerformTestsForReviewJob(branchName: it)
            createNpmPerformTestsJob(branchName: it)
            createNpmPerformCodeAnalysisForReviewJob(branchName: it)
            createNpmPerformCodeAnalysisJob(branchName: it)

            if (isAMasterBranch(it) ) {
                createNpmDeployReleaseFromMasterJob(branchName: it,
                                                    releaseTag: releaseTag,
                                                    snapshotTag: snapshotTag)
            }
            else if (isAPatchBranch(it) ) {
                String releaseVersion = it.split('-')[1]
                releaseTag += '_' +  releaseVersion
                snapshotTag += '_' +  releaseVersion

                createNpmDeployReleaseFromPatchBranchJob(branchName: it,
                                                         releaseTag: releaseTag,
                                                         snapshotTag: snapshotTag)
            }

            createNpmDeploySnapshotJob(branchName: it,
                                       snapshotTag: snapshotTag)
        }
    }

    protected void createGerritCodeFreezeJob() {
        out.println("createGerritCodeFreezeJob()")
        GerritCodeFreezeJobBuilder createGerritCodeFreezeJobBuilder = new GerritCodeFreezeJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                gerritName: 'tools/etacommon',
                jobName: projectName + "/" + projectName + '_code_freeze',
                dslFactory: dslFactory,
                repositories: getRepositoriesForCodeFreeze(),
                approvers: getCodeFreezeApprovers()
        )

        createGerritCodeFreezeJobBuilder.build()
    }

    protected void createIntegrationWashingmachineJob() {
        out.println("createIntegrationWashingmachineJob()")
        IntegrationWashinmachineJobBuilder createIntegrationWashingmachineJobBuilder = new IntegrationWashinmachineJobBuilder(
                jobName: projectName + '_integration_washingmachine',
                dslFactory: dslFactory,
                gerritServer: gerritServer,
                gerritUser: gerritUser
        )

        createIntegrationWashingmachineJobBuilder.build()
    }

    protected List getRepositoriesForCodeFreeze() {
        assert 'Method' == 'not implemented in sub-class'
        return null
    }

    protected String getCodeFreezeApprovers() {
        assert 'Method' == 'not implemented in sub-class'
        return null
    }

    protected List<String> getBranchesList(String repositoryUrl) {
        String lsRemoteCmd = '/opt/local/dev_tools/git/latest/bin/git ls-remote --heads ' + repositoryUrl
        String output = lsRemoteCmd.execute().getText()
        List<String> outputLines = output.tokenize('\n')
        branches = []

        outputLines.each { line ->
            branches.add( (line =~"refs/heads/(.*)")[0][1] )
        }

        return branches
    }

    protected boolean isAPatchBranch(String branch) {
        return branch =~ /^patch-\d+\.\d+\.x$/
    }

    protected boolean isAMasterBranch(String branch) {
        return branch =~ /^master$/
    }
}
