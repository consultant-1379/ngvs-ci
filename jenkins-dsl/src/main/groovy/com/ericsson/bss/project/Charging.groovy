package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.AutoAddReviewerJobBuilder
import com.ericsson.bss.job.charging.ChargingCreateReleaseBranchWashingMachineJobBuilder
import com.ericsson.bss.job.charging.ChargingEpValidatorJobBuilder
import com.ericsson.bss.job.charging.ChargingGerritIntegrationTestJobBuilder
import com.ericsson.bss.job.charging.ChargingGerritJiveClusterUpgradeJobBuilder
import com.ericsson.bss.job.charging.ChargingGerritSonarJobBuilder
import com.ericsson.bss.job.charging.ChargingTargethostInstallJobBuilder
import com.ericsson.bss.job.charging.ChargingUmiTestJobBuilder
import com.ericsson.bss.job.charging.ChargingWashingMachineReleaseBranchJobBuilder
import com.ericsson.bss.job.charginggui.ChargingGuiGerritUnitTestJobBuilder
import com.ericsson.bss.job.charginggui.ChargingGuiSelenideJobBuilder
import com.ericsson.bss.job.GerritUnitTestCheckstyleJobBuilder
import com.ericsson.bss.job.MultiRepositoryReleaseJobBuilder
import com.ericsson.bss.job.MvnGerritUnitTestJobBuilder
import com.ericsson.bss.util.GerritUtil

class Charging extends Project {
    public static String projectName = "charging"
    private boolean symlinkWorkspace = true

    protected HashMap<String, List> valuesOfResourceProfiles = ['TestSystem':[ALLOCATED_CPU_IN_CORE, ALLOCATE_MEMORY_IN_GIGABITE * 4],
                                                                'Extended':[ALLOCATED_CPU_IN_CORE * 2, ALLOCATE_MEMORY_IN_GIGABITE * 8],
                                                                'Washingmachine':[ALLOCATED_CPU_IN_CORE * 4, ALLOCATE_MEMORY_IN_GIGABITE * 8],
                                                                'Default':['', '']]

    private String jiveMetaData = 'https://arm.epk.ericsson.se/artifactory/simple/proj-charging-release-local/com/ericsson/jive/charging/tests/'
    private String versionLocation = 'https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/CHACORE/,' +
            'https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/ericsson/bss/rm/charging.core/;1.5.0'
    private String versionLocation2 = 'https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/CHAACCESS/,' +
            'https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/ericsson/bss/rm/charging.access/;1.5.0'

    public Charging(){
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)
        super.createCreateClusterJob(true)
        super.createRemoveClusterJob()
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-charging.xml"
        super.createPrepareArm2GaskJob()
        super.createUpgradeMsvCil('internal')
        super.createUpgradeGerritJiveMsvCil('internal')
        super.createGerritJiveClusterUpgradeJob(
                gerritJiveClusterUpgradeJobBuilderClass: ChargingGerritJiveClusterUpgradeJobBuilder)
    }

    @Override
    protected List getRepositories() {
        List repositories = new ArrayList()

        repositories.add("charging/com.ericsson.bss.rm.common.datatypes")
        repositories.add("charging/com.ericsson.bss.integrationtest.utils")
        repositories.add("kagitolite:3pp/copy.com.ericsson.bss.ctrl.jmx.monitor")
        repositories.add("charging/com.ericsson.bss.integrationtest.paxexam.provisioningplugin")
        repositories.add("charging/com.ericsson.bss.rm.charging.bundle")
        repositories.add("charging/com.ericsson.bss.rm.charging")
        repositories.add("kagitolite:unstable/com.ericsson.bss.rm.charging.tools")
        repositories.add('charging/com.ericsson.bss.rm.charging.config')
        repositories.add('charging/com.ericsson.bss.rm.charging.config-api')
        repositories.add('charging/com.ericsson.bss.rm.charging.site')
        repositories.add("jive/charging")
        repositories.add("eftf/charging")
        repositories.add("charging/com.ericsson.bss.rm.charging.shared")
        repositories.add("charging/com.ericsson.bss.rm.charging.serializer")
        repositories.add("charging/com.ericsson.bss.rm.charging.routing")
        repositories.add("kagitolite:tools/com.ericsson.bss.rm.charging.edmsim")
        repositories.add("kagitolite:tools/com.ericsson.bss.rm.charging.deserializer")
        repositories.add("charging/com.ericsson.bss.rm.charging.gui")
        repositories.add("charging/com.ericsson.bss.rm.charging.common.oam")
        repositories.add("charging/com.ericsson.bss.rm.charging.integration.config")
        repositories.add("charging/com.ericsson.bss.rm.charging.integration.testutils")
        repositories.add("charging/com.ericsson.bss.rm.charging.releasenotegenerator")
        repositories.add("charging/com.ericsson.bss.rm.charging.ep")
        repositories.add("charging/com.ericsson.bss.rm.charging.buildmaster.tools")

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected List getRepositoriesForGerritJive() {
        List repositories = []
        repositories.add("jive/charging")

        out.println("Repositories for gerrit jive: " + repositories)

        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        if (repository.equals("jive/charging")) {
            repository = "jive-charging"
        }
        else if (repository.equals("eftf/charging")) {
            repository = "eftf-charging"
        }

        String currentJobName = super.getJobName(repository)

        currentJobName = currentJobName.replace('com.ericsson.bss.', '')
        currentJobName = currentJobName.replace('rm.', '')

        return currentJobName
    }

    @Override
    @SuppressWarnings('DuplicateListLiteral')
    public void create(parent) {
        super.create(parent)
        createReleaseBranchWashingMachineJobBuilder(true)
        super.removeReleaseBranchWashingMachineJobBuilder()
        super.createOvfBuildJob(
            'Charging/Build%20Charging%20\${__VARIANT__}%20OVF',
            'suites/installnode/build_ovf.xml',
            'build_ovf_\${TARGETHOST}.xml',
            ['core', 'access', 'dlb'],
            ['core':'CHACORE', 'access':'CHAACCESS', 'dlb':'CHADLB']
        )
        createWashingMachineReleaseBranch(
            washingMachineReleaseBranchJobBuilderClass: ChargingWashingMachineReleaseBranchJobBuilder,
            mailingList: 'charging_washingmachine@mailman.lmera.ericsson.se',
            defaultTapasJobPath: 'Charging/Charging%20Washingmachine%20Releasebranch',
            suite: 'suites/installnode/washingmachine_branch.xml',
            useDlb: true,
        )
        super.createUmiTestJob( defaultTapasJobPath: 'Charging/UMI%20Charging%20Test',
                                suite: 'suites/installnode/umi_test.xml',
                                suiteFile: 'umi_test_\${TARGETHOST}-${TARGETHOST2}.xml',
                                useTwoTargethosts: true,
                                umiTestJobBuilderClass: ChargingUmiTestJobBuilder)
        createMultiRepositoryReleaseJob()
        super.createAddRemoveNightlyFullTargethostInstall(
            useTwoTargethosts: true,
            valuesOfResourceProfiles: valuesOfResourceProfiles
        )
        super.createNightlyFullTargethostInstall(
            cronTrigger: 0,
            msvResourceProfile: "TeamMachine",
            cilResourceProfile: "TeamMachine",
            targethostInstallParameters:
            [
                "CHARGINGCORE": "targethost",
                "CHARGINGACCESS": "targethost2"
            ]
        )
        super.createSetMsvCilVersion()
        super.createAddRemoveNightlyWashingmachineMsvCilUpgrade(true)
        super.createNightlyWashingmachineMsvCilUpgradeJob(
            msvResourceProfile: "Washingmachine",
            cilResourceProfile: "Washingmachine"
        )
        super.createCilTargethostRollbackJob()
        super.createWashingMachineUpgradeJob(
            defaultTapasJobPath:'Charging/Charging%20Washingmachine%20Upgrade',
            uniqueConfigIdentifier:'${COREHOST}-${ACCESSHOST}',
            variantArtifacts:
            [
                [name:'CORE',   groupid:'com.ericsson.bss.rm',  artifactid:'charging.core'],
                [name:'ACCESS', groupid:'com.ericsson.bss.rm',  artifactid:'charging.access'],
                [name:'DLB',    groupid:'com.ericsson.bss.rm',  artifactid:'charging.dlb']
            ],
            targetHosts:
            [
                [name:'MSV',                defaultValue:'vma-cha0041', description:''],
                [name:'CIL',                defaultValue:'vma-cha0042', description:''],
                [name:'COREHOST',           defaultValue:'vma-cha0043', description:'Core host we upgrade from.'],
                [name:'ACCESSHOST',         defaultValue:'vma-cha0044', description:'Access host we upgrade from.'],
                [name:'DLBHOST',            defaultValue:'vma-cha0045', description:'Dlb host on which we perform upgrade.'],
                [name:'COREHOSTSPARE',      defaultValue:'vma-cha0046', description:'Core host we upgrade to.'],
                [name:'ACCESSHOSTSPARE',    defaultValue:'vma-cha0047', description:'Access host we upgrade to.']
            ],
            suite: 'suites/installnode/washingmachine_upgrade.xml'
        )
        super.createWashingMachineKeepAliveJob(suffix:'_upgrade')
        super.createWashingMachineOnOffJob(
            suffix:'_upgrade',
            recipient:'charging_washingmachine@mailman.lmera.ericsson.se',
            variantArtifacts:
            [
                [name:'CORE_FROM_VERSION', groupid:'com.ericsson.bss.rm', artifactid:'charging.core',
                    types: ['release', 'dev'], desc: "Version of a base image too upgrade from."],
                [name:'CORE_TO_VERSION', groupid:'com.ericsson.bss.rm', artifactid:'charging.core',
                    types: ['release', 'dev'], desc: "Version of a base image too upgrade to."],
                [name:'ACCESS_FROM_VERSION', groupid:'com.ericsson.bss.rm', artifactid:'charging.access',
                    types: ['release', 'dev'], desc: "Version of a base image too upgrade from."],
                [name:'ACCESS_TO_VERSION', groupid:'com.ericsson.bss.rm', artifactid:'charging.access',
                    types: ['release', 'dev'], desc: "Version of a base image too upgrade to."],
                [name:'DLB_FROM_VERSION', groupid:'com.ericsson.bss.rm', artifactid:'charging.dlb',
                    types: ['release', 'dev'], desc: "Version of a base image too upgrade from."],
                [name:'DLB_TO_VERSION', groupid:'com.ericsson.bss.rm', artifactid:'charging.dlb',
                    types: ['release', 'dev'], desc: "Version of a base image too upgrade to."],
                [name:'JIVEVERSION', groupid:'com.ericsson.jive.charging', artifactid:'tests',
                    types: ['release', 'dev'], desc: "Jive version."],
            ]
        )
        createEpValidatorJob(
                epValidatorJobBuilderClass: ChargingEpValidatorJobBuilder,
                versionLocation: versionLocation,
                versionLocation2: versionLocation2,
                valuesOfResourceProfiles: valuesOfResourceProfiles,
                defaultTapasJobPath: 'Charging/Charging%20EP%20Verification',
                useJiveTests: true,
                jiveMetaData: jiveMetaData,
                useDvFile: true,
                useTwoTargethosts: true,
                suite: 'suites/installnode/ep_validator.xml'
        )

        super.createGerritCodeFreezeJob()
        super.createTargethostInstallJob(
            targethostInstallJobBuilderClass: ChargingTargethostInstallJobBuilder,
            versionLocation: ['https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/ericsson/bss/rm/charging.core/;1.4.0',
                              'https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/ericsson/bss/rm/charging.access/;1.4.0',
                              'https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/ericsson/bss/rm/charging.dlb/;1.4.0'],
            defaultTapasJobPath: 'CHARGING/Charging%20Targethost%20Install',
            targethostDescription: ['The machine(s) that should be installed with Charging "core" if INSTALLTYPE in [full, full+dlb, access].' +
                                   'If INSTALLTYPE is "access" this is the "core" node "access" will be configured towards.\n' +
                                   'Multiple machines separated by semicolon, e.g. vmx-XXX;vmx-XXX ',
                                   'The machine(s) that should be installed with Charging "access" if INSTALLTYPE in [full, full+dlb, access].' +
                                    ' If INSTALLTYPE is "core" this is the "access" node "core" will be configured towards.\n' +
                                    'Multiple machines separated by semicolon, e.g. vmx-XXX;vmx-XXX',
                                    'Optional. The machine(s) that should be installed with Charging dlb if INSTALLTYPE in [full+dlb, dlb].\n' +
                                    'Multiple machines separated by semicolon, e.g. vmx-XXX;vmx-XXX'],
            ovfPacName: ['CHACORE', 'CHAACCESS', 'CHADLB'],
            valuesOfResourceProfiles: ['TeamMachine':[ALLOCATED_CPU_IN_CORE * 2, ALLOCATE_MEMORY_IN_GIGABITE * 6,
                                                      ALLOCATED_CPU_IN_CORE * 2, ALLOCATE_MEMORY_IN_GIGABITE * 6,
                                                      ALLOCATED_CPU_IN_CORE * 2, ALLOCATE_MEMORY_IN_GIGABITE * 6],
                                       'TestSystem':[ALLOCATED_CPU_IN_CORE * 2, ALLOCATE_MEMORY_IN_GIGABITE * 10,
                                                     ALLOCATED_CPU_IN_CORE * 2, ALLOCATE_MEMORY_IN_GIGABITE * 4,
                                                     ALLOCATED_CPU_IN_CORE * 2, ALLOCATE_MEMORY_IN_GIGABITE * 4],
                                       'Eco':[ALLOCATED_CPU_IN_CORE * 2, ALLOCATE_MEMORY_IN_GIGABITE * 10,
                                              ALLOCATED_CPU_IN_CORE * 2, ALLOCATE_MEMORY_IN_GIGABITE * 4,
                                              ALLOCATED_CPU_IN_CORE * 2, ALLOCATE_MEMORY_IN_GIGABITE * 4],
                                       'Washingmachine':[ALLOCATED_CPU_IN_CORE * 4, ALLOCATE_MEMORY_IN_GIGABITE * 6,
                                                         ALLOCATED_CPU_IN_CORE * 4, ALLOCATE_MEMORY_IN_GIGABITE * 6,
                                                         ALLOCATED_CPU_IN_CORE * 4, ALLOCATE_MEMORY_IN_GIGABITE * 6],
                                       'Default':['', '', '', '', '', '']],
            installType: ['full', 'core', 'access', 'full+dlb', 'dlb'],
            useDvFile: true,
            useJiveTests: true,
            suite: 'suites/installnode/targethost_install.xml',
            suiteTwoHosts: 'suites/installnode/targethost_install_two_hosts.xml',
            jiveMetaData: 'https://arm.epk.ericsson.se/artifactory/simple/proj-charging-release-local/com/ericsson/jive/charging/')
    }

    @Override
    protected void createForRepository() {

        releaseGoal = DEFAULT_RELEASE_GOAL
        releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL
        prepareArm2Gask = false

        if (jobName.equalsIgnoreCase('integrationtest.paxexam.provisioningplugin')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('charging')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('charging.config')) {
            String releaseParameters = ' -DpreparationGoals="install" -Darguments="-Dorg.ops4j.pax.url.mvn.settings=\${MAVEN_SETTINGS} ' +
                    '-Dorg.ops4j.pax.url.mvn.localRepository=\${MAVEN_REPOSITORY} --settings \${MAVEN_SETTINGS} -Dmaven.repo.local=\${MAVEN_REPOSITORY}"'
            releaseGoal  += ' -Dgoals=deploy' + releaseParameters
            releaseDryrunGoal += releaseParameters
        }
        else if (jobName.equalsIgnoreCase('charging.config-api')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('integrationtest_utils')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('rm_common_datatypes')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('common.datatypes')) {
            prepareArm2Gask = true
        }
        else if (jobName.equalsIgnoreCase('charging.shared')) {
            releaseGoal += ' -DuseReleaseProfile=false'
        }
        else if (jobName.equalsIgnoreCase('charging.serializer')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('charging.routing') || jobName.equalsIgnoreCase('charging.gui')) {
            String releaseParameters = ' -DpreparationGoals="install" -Darguments="-Dorg.ops4j.pax.url.mvn.settings=\${MAVEN_SETTINGS} ' +
                    '-Dorg.ops4j.pax.url.mvn.localRepository=\${MAVEN_REPOSITORY} --settings \${MAVEN_SETTINGS} -Dmaven.repo.local=\${MAVEN_REPOSITORY}"'
            releaseGoal += ' -Dgoals=deploy' + releaseParameters
            releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL + releaseParameters
        }
        else if (jobName.equalsIgnoreCase('charging.gui')) {
            String releaseParameters = ' -Darguments="-Dorg.ops4j.pax.url.mvn.settings=\${MAVEN_SETTINGS} ' +
                    '-Dorg.ops4j.pax.url.mvn.localRepository=\${MAVEN_REPOSITORY} --settings \${MAVEN_SETTINGS} -Dmaven.repo.local=\${MAVEN_REPOSITORY}"'
            releaseGoal = DEFAULT_RELEASE_GOAL + ' -Dgoals=deploy -DuseReleaseProfile=false' + releaseParameters
            releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL + releaseParameters
        }
        else if (jobName.equalsIgnoreCase('charging.common_oam')) {
            releaseGoal += ' -DuseReleaseProfile=false'
        }
        else if (jobName.equalsIgnoreCase('charging.integration.config')) {
            String releaseParameters = ' -P!rpmOs'
            releaseGoal = DEFAULT_RELEASE_GOAL + releaseParameters +' -Dgoals=deploy'
            releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL + releaseParameters
        }
        else if (jobName.equalsIgnoreCase('charging.integration.testutils')) {
            String releaseParameters = ' -P!rpmOs'
            releaseGoal = DEFAULT_RELEASE_GOAL + releaseParameters +' -Dgoals=deploy'
            releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL + releaseParameters
        }

        if (jobName.equalsIgnoreCase('charging.site')) {
            out.println("projectName: " + projectName + ", jobname: " + jobName +
                    ", gerritName: " +gerritName)

            createFolders()
            createSite()
            createSonar()
            createDeploy()
            createSonarGerrit()
            createMvnDependencyTest()
            createReleaseDeploy()
            createReleaseBranch()
            createSiteGerrit()
        }
        else {
            super.createForRepository()
            if (jobName.equalsIgnoreCase('charging_gui')){
                out.println("createSelenideTest()")
                ChargingGuiSelenideJobBuilder selenideJobBuilder = new ChargingGuiSelenideJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_selenide_test",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    symlinkWorkspace: this.symlinkWorkspace,
                    verboseGerritFeedback: verboseGerritFeedback,
                    dslFactory: dslFactory
                    )

                selenideJobBuilder.build()
            }
        }
    }

    @Override
    protected void createSite() {
        int increaseTimeout = 2

        if (jobName.equalsIgnoreCase('charging_gui')) {
            super.createSite(getDefaultJobTimeout() * increaseTimeout)
        }
        else {
            super.createSite()
        }
    }

    @Override
    protected void createAutoAddReviewer() {

        String[] reviewers = null
        switch (jobName.toLowerCase()) {
            case 'charging_routing':
            case 'charging_serializer':
            case 'charging_shared':
                reviewers = ["CFT10"]
                break
            case 'charging_bundle':
            case 'charging_integration':
                reviewers = ["\"Charging Buildmaster\""]
                break
            case 'charging_gui':
                reviewers = ["\"Access IT\""]
                break
            case 'charging_common_oam':
                reviewers = ["\"Charging common.oam Auto Reviewers\""]
                break
            case 'charging_config':
                reviewers = ["\"Charging config Auto Reviewers\""]
                break
            case 'charging_config-api':
                reviewers = ["\"Charging config-api Auto Reviewers\""]
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
    protected void createUnittestGerrit() {
        if (jobName.equalsIgnoreCase('jive-charging')) {
            MvnGerritUnitTestJobBuilder gerritUnitTestJobBuilder = new GerritUnitTestCheckstyleJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_gerrit_unit_test",
                    mavenSettingsFile: mvnSettingFile,
                    mavenRepositoryPath: mavenRepositoryPath,
                    gerritName: gerritName,
                    projectName: projectName,
                    verboseGerritFeedback: verboseGerritFeedback,
                    dslFactory: dslFactory
                    )

            gerritUnitTestJobBuilder.build()
        }
        else if (jobName.equalsIgnoreCase('charging_gui')){
            int timeoutForJob = 30
            ChargingGuiGerritUnitTestJobBuilder guiGerritUnitTestJobBuilder = new ChargingGuiGerritUnitTestJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_unit_test",
                mavenSettingsFile: mvnSettingFile,
                mavenRepositoryPath: mavenRepositoryPath,
                gerritName: gerritName,
                projectName: projectName,
                verboseGerritFeedback: verboseGerritFeedback,
                symlinkWorkspace: this.symlinkWorkspace,
                dslFactory: dslFactory,
                injectPortAllocation: "scripts/InjectPortAllocationChargingGui.groovy",
                timeoutForJob: timeoutForJob
                )

            guiGerritUnitTestJobBuilder.build()
        }
        else {
            super.createUnittestGerrit()
        }
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
                dslFactory: dslFactory
                )

        chargingGerritSonarJobBuilder.build()
    }

    @Override
    protected void createIntegrationTestGerrit() {
        if (gerritName == 'charging/com.ericsson.bss.rm.charging.bundle') {
            ChargingGerritIntegrationTestJobBuilder gerritIntegrationTestJobBuilder = new ChargingGerritIntegrationTestJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_gerrit_integration_test",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFilePath + "kascmadm-settings_arm-charging_access.xml",
                    gerritName: gerritName,
                    projectName: projectName,
                    timeoutForJob: 120,
                    verboseGerritFeedback: verboseGerritFeedback,
                    dslFactory: dslFactory
            )

            gerritIntegrationTestJobBuilder.build()
        }
    }

    private void createMultiRepositoryReleaseJob() {
        out.println("createReleaseJob()")

        String repositoriesToRelease = dslFactory.readFileFromWorkspace('scripts/pomCheckRepositoriesForChargingAccess.groovy') +
        dslFactory.readFileFromWorkspace('scripts/pomCheckRepositoriesForChargingCore.groovy') +
        'ssh://gerrit.epk.ericsson.se:29418/charging/com.ericsson.bss.rm.charging.bundle\n' +
        'ssh://gerrit.epk.ericsson.se:29418/charging/com.ericsson.bss.rm.charging.integration.config\n' +
        'ssh://gerrit.epk.ericsson.se:29418/jive/charging\n' +
        'ssh://gerrit.epk.ericsson.se:29418/charging/com.ericsson.bss.rm.charging.gui\n' +
        'ssh://gerrit.epk.ericsson.se:29418/charging/com.ericsson.bss.rm.charging.dlb\n' +
        'ssh://gerrit.epk.ericsson.se:29418/charging/com.ericsson.bss.rm.charging\n'

        MultiRepositoryReleaseJobBuilder multiRepositoryReleaseJobBuilder = new MultiRepositoryReleaseJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                projectName: projectName,
                mavenRepositoryPath: mavenRepositoryPath,
                repositoryList: repositoriesToRelease.tokenize(),
                buildName: 'charging',
                releaseRepository:'proj-charging-release-local',
                stagingRepository: 'proj-charging-staging-local',
                mail: 'bssf-charging-automation@mailman.lmera.ericsson.se',
                mavenSettingsFile: mvnSettingFilePath + "kascmadm-settings_arm-charging_access.xml",
                dslFactory: dslFactory,
                generateGUIconfig: true,
                symlinkWorkspace: true,
                extraEnvironmentVariables: getLangProperties()
                )

        multiRepositoryReleaseJobBuilder.build()
    }

    private static getLangProperties() {
        Map<String, String> variables = [:]

        variables.put("LC_ALL", "en_US.UTF-8")
        variables.put("LANG", "en_US.UTF-8")

        return variables
    }

    @Override
    protected void createReleaseBranchWashingMachineJobBuilder(boolean useTwoTargethosts = false) {
        out.println("CreateReleaseBranchWashingMachineJobBuilder()")
        ChargingCreateReleaseBranchWashingMachineJobBuilder createReleaseBranchWashingMachineJobBuilder =
                                                            new ChargingCreateReleaseBranchWashingMachineJobBuilder (
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: projectName + '_create_washingmachine_releasebranch',
                projectName: projectName,
                dslFactory: dslFactory,
                runXvfb: false,
                useTwoTargethosts: useTwoTargethosts
        )

        createReleaseBranchWashingMachineJobBuilder.build()
    }

    @Override
    protected List getRepositoriesForCodeFreeze() {
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))
        List<String> repositories = []
        for (repository in output.split(System.lineSeparator())) {
            if (repository.contains("charging/com.ericsson.bss.rm.charging")) {
                repositories.add(repository)
            }
        }

        // Removing unused repositories
        repositories.remove("charging/com.ericsson.bss.rm.charging")
        repositories.remove("charging/com.ericsson.bss.rm.charging.demosuite")
        repositories.remove("charging/com.ericsson.bss.rm.charging.devenv")
        repositories.remove("charging/com.ericsson.bss.rm.charging.edmsim")
        repositories.remove("charging/com.ericsson.bss.rm.charging.integrationtest")
        repositories.remove("charging/com.ericsson.bss.rm.charging.learning")
        repositories.remove("charging/com.ericsson.bss.rm.charging.nft.nft")
        repositories.remove("charging/com.ericsson.bss.rm.charging.prerating")
        repositories.remove("charging/com.ericsson.bss.rm.charging.release")
        repositories.remove("charging/com.ericsson.bss.rm.charging.testtools")
        repositories.remove("charging/com.ericsson.bss.rm.charging.tools")
        repositories.remove("charging/com.ericsson.bss.rm.charging.access.tools")
        repositories.remove("charging/com.ericsson.bss.rm.charging.deserializer")
        repositories.remove("charging/com.ericsson.bss.rm.charging.configurations")
        repositories.remove("charging/com.ericsson.bss.rm.charging.access.server")
        repositories.remove("charging/com.ericsson.bss.rm.charging.access.oam.health")
        repositories.remove("charging/com.ericsson.bss.rm.charging.access.cache")

        return repositories
    }

    @Override
    protected String getCodeFreezeApprovers() {
        '''
        Gerrit account ids for user:
        emikasb Mikael Åsberg - 1174
        eadamkr Adam Krauser  -  869
        '''
        return "1174 869"
    }
}
