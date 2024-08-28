package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.AutoAddReviewerJobBuilder
import com.ericsson.bss.job.DeployJobBuilder
import com.ericsson.bss.job.GerritIntegrationTestJobBuilder
import com.ericsson.bss.job.chargingcore.ChargingCoreIntegrationTestGerritSiteJobBuilder
import com.ericsson.bss.job.GerritSonarJobBuilder
import com.ericsson.bss.job.MvnGerritUnitTestJobBuilder
import com.ericsson.bss.job.chargingcore.ChargingCoreIntegrationTestSiteJobBuilder
import com.ericsson.bss.job.SonarJobBuilder
import com.ericsson.bss.job.charging.ChargingGerritSonarJobBuilder
import com.ericsson.bss.job.chargingcore.ChargingCoreCreateSoftwareRecordDeploy
import com.ericsson.bss.job.chargingcore.ChargingCoreGerritUnitTestCheckstyleJobBuilder
import com.ericsson.bss.job.chargingcore.ChargingCoreIntegrationTestTraceInstallJobBuilder
import com.ericsson.bss.job.chargingcore.ChargingCoreSonarJobBuilder
import com.ericsson.bss.job.custom.InstallJobBuilder
import com.ericsson.bss.util.GerritUtil

class ChargingCore extends Project {

    public static final int INTEGRATION_TESTS_TIMEOUT = 50

    public String projectName = "charging.core"
    public String blameMailList = 'bssf-charging-automation@mailman.lmera.ericsson.se'

    private static final String INTEGRATION_TEST_PROFILE = 'attachCoverage'

    private int sonarTimeout = 60

    public ChargingCore(){
        super.projectName = this.projectName
        super.blameMailList = this.blameMailList
    }

    @Override
    public void init(parent) {
        super.init(parent)

        integrationTestRepository = gerritUser + "@" + gerritServer + ":29418/charging/com.ericsson.bss.rm.charging.integrationtest"
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-charging.xml"
    }

    @Override
    public void create(parent) {
        super.create(parent)
        super.createSoftwareRecordJob()
        super.createInstallRpmJob('Charging/Charging%20Core%20Targethost%20Install%20RPM',
                'suites/installnode/targethost_install_rpm.xml',
                'targethost_install_rpm_\${TARGETHOST}.xml',
                'core')
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = new ArrayList()
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains('com.ericsson.bss.rm.charging') && !repository.contains('charging.access')){
                repositories.add(repository)
            }
        }

        repositories.remove('charging/com.ericsson.bss.rm.charging.bundle')
        repositories.remove('charging/com.ericsson.bss.rm.charging')
        repositories.remove('charging/com.ericsson.bss.rm.charging.cipdiameter')
        repositories.remove('charging/com.ericsson.bss.rm.charging.duplicatedetection')
        repositories.remove('charging/com.ericsson.bss.rm.charging.busstation')
        repositories.remove('charging/com.ericsson.bss.rm.charging.integration.installconfig')
        repositories.remove('charging/com.ericsson.bss.rm.charging.rerating')
        repositories.remove('poc/com.ericsson.bss.rm.charging.rerating')
        repositories.remove('charging/com.ericsson.bss.rm.charging.dlb')
        repositories.remove('charging/com.ericsson.bss.rm.charging.config')
        repositories.remove('charging/com.ericsson.bss.rm.charging.config-api')
        repositories.remove('charging/com.ericsson.bss.rm.charging.site')
        repositories.remove('charging/com.ericsson.bss.rm.charging.shared')
        repositories.remove('charging/com.ericsson.bss.rm.charging.serializer')
        repositories.remove('charging/com.ericsson.bss.rm.charging.testtools')
        repositories.remove('charging/com.ericsson.bss.rm.charging.routing')
        repositories.remove('charging/com.ericsson.bss.rm.charging.gui')
        repositories.remove('charging/com.ericsson.bss.rm.charging.common.oam')
        repositories.remove('charging/com.ericsson.bss.rm.charging.prerating')
        repositories.remove('charging/com.ericsson.bss.rm.charging.integration.config')
        repositories.remove('charging/com.ericsson.bss.rm.charging.integration.testutils')
        repositories.remove('charging/com.ericsson.bss.rm.charging.umi.datacollector')
        repositories.remove('charging/com.ericsson.bss.rm.charging.devenv')
        repositories.remove('charging/com.ericsson.bss.rm.charging.ep')
        repositories.remove('charging/com.ericsson.bss.rm.charging.buildmaster.tools')
        repositories.remove('charging/com.ericsson.bss.rm.charging.top')

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected List getRepositoriesForGerritJive() {
        List repositories = []
        repositories.add("charging/com.ericsson.bss.rm.charging.bucketmanagement")
        repositories.add("charging/com.ericsson.bss.rm.charging.capability")
        repositories.add("charging/com.ericsson.bss.rm.charging.cel")
        repositories.add("charging/com.ericsson.bss.rm.charging.common")
        repositories.add("charging/com.ericsson.bss.rm.charging.configcache")
        repositories.add("charging/com.ericsson.bss.rm.charging.core.genericinterfaceparameters")
        repositories.add("charging/com.ericsson.bss.rm.charging.customermanagement")
        repositories.add("charging/com.ericsson.bss.rm.charging.dataaccess")
        repositories.add("charging/com.ericsson.bss.rm.charging.dynamicfunction")
        repositories.add("charging/com.ericsson.bss.rm.charging.event")
        repositories.add("charging/com.ericsson.bss.rm.charging.globalconfig")
        repositories.add("charging/com.ericsson.bss.rm.charging.invoiceaggregation")
        repositories.add("charging/com.ericsson.bss.rm.charging.invoiceaggregation.customerbillingcycle")
        repositories.add("charging/com.ericsson.bss.rm.charging.oam")
        repositories.add("charging/com.ericsson.bss.rm.charging.periodicaction")
        repositories.add("charging/com.ericsson.bss.rm.charging.productselection")
        repositories.add("charging/com.ericsson.bss.rm.charging.productstatus")
        repositories.add("charging/com.ericsson.bss.rm.charging.race")
        repositories.add("charging/com.ericsson.bss.rm.charging.reasoncode")
        repositories.add("charging/com.ericsson.bss.rm.charging.refill")
        repositories.add("charging/com.ericsson.bss.rm.charging.rf.core")
        repositories.add("charging/com.ericsson.bss.rm.charging.runtimeflow")
        repositories.add("charging/com.ericsson.bss.rm.charging.servicedetermination")
        repositories.add("charging/com.ericsson.bss.rm.charging.serviceprovider")
        repositories.add("charging/com.ericsson.bss.rm.charging.services")
        repositories.add("charging/com.ericsson.bss.rm.charging.session")
        repositories.add("charging/com.ericsson.bss.rm.charging.transport")
        repositories.add("charging/com.ericsson.bss.rm.charging.vre")

        out.println("Repositories for gerrit jive: " + repositories)

        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        String currentJobName = super.getJobName(repository)

        currentJobName = currentJobName.replace('com.ericsson.bss.rm.charging.', '')

        return currentJobName
    }

    @Override
    protected createJiveTestGerrit() {
        super.createJiveTestGerrit("scripts/gerrit_deploy_to_karaf_then_jive.sh", 45)
    }

    protected void createDeploy() {
        out.println("createDeploy()")

        releaseGoal = DEFAULT_RELEASE_GOAL
        releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL

        int timeoutForJob = 25

        if (hasInternalIntegrationTest()) {
            addIntegrationTestReleaseParameters()
        }

        if (jobName.equalsIgnoreCase('capability')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('cil')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('configcache')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('configurations')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('customermanagement')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('dataaccess')) {
            releaseGoal += ' -Dgoals=deploy -DuseReleaseProfile=false'
        }
        else if (jobName.equalsIgnoreCase('demosuite')) {
            releaseGoal += ' -Dgoals=deploy -DuseReleaseProfile=false'
        }
        else if (jobName.equalsIgnoreCase('dlb')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('dynamicfunction')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('event')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('functioncontrol')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('globalconfig')) {
            String releaseParameters = ' -DpreparationGoals="install" -Darguments="-Dorg.ops4j.pax.url.mvn.settings=\${MAVEN_SETTINGS} ' +
                    '-Dorg.ops4j.pax.url.mvn.localRepository=\${MAVEN_REPOSITORY} --settings \${MAVEN_SETTINGS} -Dmaven.repo.local=\${MAVEN_REPOSITORY}"'
            releaseGoal  += ' -Dgoals=deploy' + releaseParameters
            releaseDryrunGoal += releaseParameters
        }
        else if (jobName.equalsIgnoreCase('integration')) {
            releaseGoal += ' -P!rpmOs -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('integrationtest')) {
            releaseGoal += ' -Dgoals=deploy -DpublishToJivePortal=true'
        }
        else if (jobName.equalsIgnoreCase('invoiceaggregation')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('oam')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('periodicaction')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('productiondependencies')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('productselection')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('race')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('reasoncode')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('rf.core')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('runtimeflow')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('servicedetermination')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('serviceprovider')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('services')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('session')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('testtools')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('transport')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('umi_datacollector')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('umi')) {
            releaseGoal += ' -Dgoals=deploy'
        }

        if (jobName.equalsIgnoreCase('integrationtest')) {
            InstallJobBuilder installTrace = new ChargingCoreIntegrationTestTraceInstallJobBuilder(
                    workspacePath: workspacePath,
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    extraMavenParameters: '-Dtrace=true',
                    jobName: folderName + "/" + jobName + "_trace",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    timeoutForJob: INTEGRATION_TESTS_TIMEOUT, // Ticket-Id: #6879 Temporary
                    projectName: projectName,
                    profilesToBeUsed: INTEGRATION_TEST_PROFILE,
                    dslFactory: dslFactory,
                    )

            installTrace.build()

            InstallJobBuilder installSampleConfig = new InstallJobBuilder(
                    workspacePath: workspacePath,
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    extraMavenParameters: '-DsampleConfigTests=true',
                    jobName: folderName + "/" + jobName + "_sampleconfig",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    timeoutForJob: INTEGRATION_TESTS_TIMEOUT, // Ticket-Id: #6879 Temporary
                    projectName: projectName,
                    profilesToBeUsed: INTEGRATION_TEST_PROFILE,
                    dslFactory: dslFactory,
                    )

            installSampleConfig.build()

            InstallJobBuilder installKaraf = new InstallJobBuilder(
                    workspacePath: workspacePath,
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    extraMavenParameters: '-DkeepRuntimeFolder=false -DdeployToTemporaryStore=true',
                    jobName: folderName + "/" + jobName + "_karaf",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    timeoutForJob: timeoutForJob,
                    projectName: projectName,
                    dslFactory: dslFactory
                    )

            installKaraf.buildReleaseBranch('karaf')

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
                    timeoutForJob: INTEGRATION_TESTS_TIMEOUT, // Ticket-Id: #6879 Temporary
                    extraMavenParameters: "-DpublishToJivePortal=true -DgenerateTestCoverage=true"
                    )

            deployJobBuilder.build()
        }
        else if (jobName.equalsIgnoreCase('integration')) {
            DeployJobBuilder deployJobBuilder = new ChargingCoreCreateSoftwareRecordDeploy(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    releaseGoal: releaseGoal,
                    releaseDryrunGoal: releaseDryrunGoal,
                    jobName: folderName + "/" + jobName + "_deploy",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    timeoutForJob: timeoutForJob,
                    generateSoftwareRecord: true,
                    projectName: projectName,
                    dslFactory: dslFactory,
                    blameMailList: blameMailList
                    )

            deployJobBuilder.build()
        }
        else {
            super.createDeploy()
        }
    }

    private boolean hasInternalIntegrationTest() {
        return jobName.equalsIgnoreCase('capability')
    }

    private void addIntegrationTestReleaseParameters() {
        String releaseParameters = ' -DpreparationGoals="install" -Darguments="-Dorg.ops4j.pax.url.mvn.settings=\${MAVEN_SETTINGS} ' +
                '-Dorg.ops4j.pax.url.mvn.localRepository=\${MAVEN_REPOSITORY} --settings \${MAVEN_SETTINGS} -Dmaven.repo.local=\${MAVEN_REPOSITORY}"'
        releaseGoal  += ' -Dgoals=deploy' + releaseParameters
        releaseDryrunGoal += releaseParameters
    }

    @Override
    protected void createSite() {
        int increaseTimeout = 2

        if (jobName.equalsIgnoreCase('integrationtest')) {
            out.println("createSite()")
            ChargingCoreIntegrationTestSiteJobBuilder siteJobBuilder = new ChargingCoreIntegrationTestSiteJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_site",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    dslFactory: dslFactory,
                    generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                    extraMavenOptions: " install -pl testcases -Dtest=RunTimeFlowPictureCreatorTest",
                    timeoutForJob: getDefaultJobTimeout() * increaseTimeout
                    )

            siteJobBuilder.build()
        }
        else if (jobName.equalsIgnoreCase('services')) {
            super.createSite(getDefaultJobTimeout() * increaseTimeout)
        }
        else{
            super.createSite()
        }
    }

    @Override
    protected List getPomCheckRepositoryList(){
        String pomCheckRepositoryList = dslFactory.readFileFromWorkspace('scripts/pomCheckRepositoriesForChargingCore.groovy')
        return pomCheckRepositoryList.replaceAll("ssh://gerrit.epk.ericsson.se:29418/", "").split("\\n")
    }

    @Override
    protected void createSiteGerrit() {
        if (jobName.equalsIgnoreCase('integrationtest')) {
            out.println("createSiteGerrit()")
            ChargingCoreIntegrationTestGerritSiteJobBuilder gerritSiteJobBuilder = new ChargingCoreIntegrationTestGerritSiteJobBuilder(
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
                    extraMavenOptions: " install -pl testcases -Dtest=RunTimeFlowPictureCreatorTest"
                    )

            gerritSiteJobBuilder.build()
        }
        else{
            super.createSiteGerrit()
        }
    }

    @Override
    protected void createUnittestGerrit() {
        if (jobName.equalsIgnoreCase('integrationtest')) {
            MvnGerritUnitTestJobBuilder gerritUnitTestJobBuilder = new ChargingCoreGerritUnitTestCheckstyleJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_gerrit_unit_test",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    projectName: projectName,
                    verboseGerritFeedback: verboseGerritFeedback,
                    timeoutForJob: INTEGRATION_TESTS_TIMEOUT, // Ticket-Id: #6879 Temporary
                    dslFactory: dslFactory
                    )

            gerritUnitTestJobBuilder.build()
        }
        else {
            super.createUnittestGerrit()
        }
    }

    @Override
    protected void createIntegrationTestGerrit(){
        int timeoutForJob = 120

        if (!jobName.equalsIgnoreCase('core_ui') &&
            !jobName.equalsIgnoreCase('core_clamshell') &&
            !jobName.equalsIgnoreCase('integration_testutils') &&
            !jobName.equalsIgnoreCase('releasenotegenerator') &&
            !jobName.equalsIgnoreCase('release') &&
            !jobName.equalsIgnoreCase('cli') &&
            !jobName.equalsIgnoreCase('config_schemas')
        ){
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
                        timeoutForJob: timeoutForJob,
                        dslFactory: dslFactory
                        )

                gerritIntegrationTestJobBuilder.build()
            }
        }
    }

    @Override
    protected void createSonar() {
        SonarJobBuilder sonarJobBuilder
        out.println("[info] createSonar()")
        if (jobName.equalsIgnoreCase('integrationtest')) {
            sonarJobBuilder = new SonarJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_sonar",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    profilesToBeUsed: INTEGRATION_TEST_PROFILE,
                    timeoutForJob: sonarTimeout,
                    dslFactory: dslFactory
            )
        }
        else {
            sonarJobBuilder = new ChargingCoreSonarJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_sonar",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    timeoutForJob: sonarTimeout,
                    dslFactory: dslFactory
            )
        }

        sonarJobBuilder.build()
    }

    @Override
    protected void createSonarGerrit() {
        out.println("createSonarGerrit()")
        ChargingGerritSonarJobBuilder chargingGerritSonarJobBuilder = new ChargingGerritSonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                verboseGerritFeedback: verboseGerritFeedback,
                timeoutForJob: sonarTimeout,
                dslFactory: dslFactory
                )
        chargingGerritSonarJobBuilder.build()
    }

    @Override
    protected void createReleaseSonar() {

        out.println("createReleaseSonar()")

        def releaseProjects = getReleaseBranches()

        if (releaseProjects.size() != 0 && releaseProjects != null) {
            releaseProjects.each {
                def branchName = it
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
                        timeoutForJob: sonarTimeout,
                        generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName)
                        )
                sonarJobBuilder.build()
            }
        }
    }

    @Override
    protected void createReleaseSonarGerrit() {

        out.println("createReleaseSonarGerrit()")

        def releaseProjects = getReleaseBranches()

        if (releaseProjects.size() != 0 && releaseProjects != null) {
            releaseProjects.each {
                def branchName = it
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
                        timeoutForJob: sonarTimeout,
                        generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName)
                        )

                gerritSonarJobBuilder.build()
            }
        }
    }

    @Override
    protected void createAutoAddReviewer() {
        String[] reviewers = null
        String CFT4_GROUP = 'CFT4'
        String CFT38_GROUP = 'CFT38'
        String CFT26_GROUP = 'CFT26'

        switch (jobName.toLowerCase()) {
            case 'periodicaction':
            case 'serviceprovider':
            case 'session':
            case 'transport':
                reviewers = ["CFT10"]
                break
            case 'productselection':
            case 'race':
                reviewers = [CFT4_GROUP, CFT38_GROUP]
                break
            case 'core_bundle':
            case 'productiondependencies':
                reviewers = ["\"Charging Buildmaster\""]
                break
            case 'core_genericinterfaceparameters':
                reviewers = [CFT26_GROUP]
                break
            case 'services':
                reviewers = ["\"Charging services Auto Reviewers\""]
                break
            case 'cel':
                reviewers = [CFT26_GROUP]
                break
            case 'event':
                reviewers = [CFT26_GROUP]
                break
            case 'core_ui':
                reviewers = ["\"Charging core.ui Auto Reviewers\""]
                break
            case 'cli':
                reviewers = ["\"Charging cli Auto Reviewers\""]
                break
            case 'core_clamshell':
                reviewers = ["\"Charging core.clamshell Auto Reviewers\""]
                break
            case 'customermanagement':
                reviewers = [CFT26_GROUP]
                break
            case 'functioncontrol':
                reviewers = ["\"Charging functioncontrol Auto Reviewers\""]
                break
            case 'globalconfig':
                reviewers = ["\"Charging globalconfig Auto Reviewers\""]
                break
            case 'bucketmanagement':
                reviewers = ["CFT45"]
                break
            case 'dataaccess':
                reviewers = ["\"Charging dataaccess Auto Reviewers\""]
                break
            case 'refill':
                reviewers = ["\"Charging refill Auto Reviewers\""]
                break
        }

        if (reviewers != null) {
            AutoAddReviewerJobBuilder autoAddReviewerJobBuilder = new AutoAddReviewerJobBuilder(
                    gerritUser: gerritUser,
                    reviewers: reviewers,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_gerrit_add_reviewers",
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    dslFactory: dslFactory
                    )

            autoAddReviewerJobBuilder.build()
        }
    }

    @Override
    protected List getIgnoredRepositoriesForIntegrationTest() {
        return [
            'devenv',
            'integrationtest',
            'test_integration',
            'jive',
            'integration',
            'umi',
            'functioncontrol'
        ]
    }

    @Override
    protected void createReleaseDeploy(int timeout = getDefaultReleaseJobTimeout()) {
        super.createReleaseDeploy(INTEGRATION_TESTS_TIMEOUT)
    }
}
