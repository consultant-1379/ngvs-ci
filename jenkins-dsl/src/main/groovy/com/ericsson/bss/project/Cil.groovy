package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.DeployJobBuilder
import com.ericsson.bss.job.GerritSonarJobBuilder
import com.ericsson.bss.job.cil.CilGerritUnitTestJobBuilder
import com.ericsson.bss.job.SonarJobBuilder
import com.ericsson.bss.job.cil.CilTargethostInstallJobBuilder
import com.ericsson.bss.job.cil.CilCreateSoftwareRecordDeploy
import com.ericsson.bss.job.cil.CilServerGerritSonarJobBuilder
import com.ericsson.bss.job.cil.CilServerGerritUnitTestJobBuilder
import com.ericsson.bss.job.cil.CilServerSonarJobBuilder
import com.ericsson.bss.job.cil.CilUmiTestJobBuilder

class Cil extends Project {
    public static String projectName = "cil"
    private boolean symlinkWorkspace = true
    private List duplicationList = ['2', '8192']
    private HashMap<String, List> valuesOfResourceProfiles = ['TeamMachine': duplicationList,
                                                                'Washingmachine': duplicationList, 'TestSystem': ['4', '8192'], 'Standard': ['', '']]
    private String resourceProfilesDescription = (
        'Specifies how much hardware resources (CPU and RAM) the target hosts should be deployed with.\n' +
        'Normally the "TeamMachine" profile should be used.\n\n' +
        '"TeamMachine profile" should be used for "single shot" traffic.\n\n' +
        '"Washingmachine profile" should be use for the washing machines.\n\n' +
        '"TestSystem profile" should be used for small "RM" tests systems up to 100 000 subscribers.\n\n' +
        '"Standard profile" is used to install a standard production system.\n\n')

    public Cil(){
        super.projectName = this.projectName
        super.releaseGoal = "-Dresume=false release:prepare release:perform -DuseReleaseProfile=false"
        super.releaseDryrunGoal = "-Dresume=false -DdryRun=true release:prepare -DuseReleaseProfile=false"
    }

    @Override
    public void init(parent) {
        super.init(parent)

        if (System.getProperty("user.name").equalsIgnoreCase("kascmadm")) {
            gerritUser = new String("chargingsystem_local")
        }
        gerritServer = GERRIT_FORGE_SERVER

        integrationTestRepository = gerritUser + "@" + gerritServer + ":29418/cil_test_integration"

        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-cil.xml"
        delimiter = '_'
    }

    @Override
    public void create(parent) {
        super.create(parent)
        super.createSoftwareRecordJob()
        super.createCreateClusterJob(false, [], false)
        super.createRemoveClusterJob()
        super.createOvfBuildJob(
            'CIL/Build%20CIL%20OVF',
            'suites/installnode/build_ovf.xml',
            'build_ovf_\${TARGETHOST}.xml'
        )
        super.createUmiTestJob( defaultTapasJobPath: 'CIL/UMI%20CIL%20Test',
                                suite: 'suites/installnode/umi_test.xml',
                                suiteFile: 'umi_test_\${TARGETHOST}.xml',
                                umiTestJobBuilderClass: CilUmiTestJobBuilder,
                                runXvfb: true)
        super.createTargethostInstallJob(
            installNodeName: 'vmx-cil019',
            versionLocation: ['https://arm.epk.ericsson.se/artifactory/proj-cil-release-local/com/ericsson/bss/cil/server/cil-server-dv,' +
                             'https://arm.epk.ericsson.se/artifactory/proj-cil-release-local/com/ericsson/bss/cil/server/cil-staging/'],
            defaultTapasJobPath: 'CIL/CIL%20Targethost%20Install',
            suite: 'suites/installnode/targethost_install.xml',
            valuesOfResourceProfiles: valuesOfResourceProfiles,
            resourceProfilesDescription: resourceProfilesDescription,
            targethostDescription: ['The machine(s) that should be deployed, if multiple machines, use \"<b>;</b>\" to separate them. Ex. vmx123;vmx456'],
            useCil: false,
            targethostInstallJobBuilderClass: CilTargethostInstallJobBuilder,
            useDvFile: true
        )
        super.createTargethostUpgradeJob(
            versionLocation: 'https://arm.epk.ericsson.se/artifactory/proj-cil-release-local/com/ericsson/bss/cil/server/cil-server-dv,' +
                             'https://arm.epk.ericsson.se/artifactory/proj-cil-release-local/com/ericsson/bss/cil/server/cil-staging/',
            defaultTapasJobPath: 'CIL/CIL%20Targethost%20Upgrade',
            suite: 'suites/installnode/targethost_upgrade.xml',
            useCil: false
        )
        super.createAddRemoveNightlyFullTargethostInstall(
            onlyTargethostInstallation: true,
            useCilMachine: false,
            valuesOfResourceProfiles: valuesOfResourceProfiles
        )
        super.createNightlyFullTargethostInstall(
            cronTrigger: 0,
            targethostInstallParameters:
            [
             "TARGETHOST": "targethost"
            ]
        )
    }

    @Override
    protected void createFolders() {
        super.createFolders()
        dslFactory.folder(folderName + "/feature") { }
    }

    @Override
    protected void createForRepository() {
        super.createForRepository()
        super.createFeatureDeploy()
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = new ArrayList()

        String output = getGerritforgeProjects()

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains(projectName + '_')){
                repositories.add(repository)
            }
        }

        repositories.remove('cil_internal')
        repositories.remove('cil_cdal_coba')
        repositories.remove('cil_cdal_cpm')
        repositories.remove('cil_dev')
        repositories.remove('cil_gui')
        repositories.remove('cil_oam')
        repositories.remove('cil_umi')
        repositories.remove('cil_umi_top')
        repositories.remove('cil_umi_common')

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected List getGuiRepositories() {
        List<String> repositories = []
        repositories.add("tools")
        return repositories
    }

    @Override
    protected void createSonar() {
        if (jobName.equals("tools")) {
            out.println("createSonar()")

            Map extraEnvVars = [:]
            extraEnvVars.put('CIL_CLI_CONFIG_DIR', '\${WORKSPACE}')

            SonarJobBuilder sonarJobBuilder = new SonarJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_sonar",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    dslFactory: dslFactory,
                    extraEnvironmentVariables: extraEnvVars,
                    generateGUIconfig: true
                    )
            sonarJobBuilder.build()
        } else if (jobName.equals("server")){
            out.println("createSonar()")

            CilServerSonarJobBuilder sonarJobBuilder = new CilServerSonarJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_sonar",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    symlinkWorkspace: this.symlinkWorkspace,
                    dslFactory: dslFactory
                    )
            sonarJobBuilder.build()
        }
        else {
            super.createSonar()
        }
    }

    @Override
    protected void createSonarGerrit() {
        if (jobName.equals("tools")) {
            out.println("createSonarGerrit()")

            Map extraEnvVars = [:]
            extraEnvVars.put('CIL_CLI_CONFIG_DIR', '\${WORKSPACE}')

            GerritSonarJobBuilder gerritSonarJobBuilder = new GerritSonarJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_gerrit_sonar",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    projectName: projectName,
                    verboseGerritFeedback: verboseGerritFeedback,
                    dslFactory: dslFactory,
                    extraEnvironmentVariables: extraEnvVars,
                    generateGUIconfig: true
                    )
            gerritSonarJobBuilder.build()
        } else if (jobName.equals("server")){
            out.println("createSonarGerrit()")
            CilServerGerritSonarJobBuilder gerritSonarJobBuilder = new CilServerGerritSonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                verboseGerritFeedback: verboseGerritFeedback,
                symlinkWorkspace: this.symlinkWorkspace,
                dslFactory: dslFactory
                )
            gerritSonarJobBuilder.build()
        }
        else {
            super.createSonarGerrit()
        }
    }

    @Override
    protected void createUnittestGerrit() {
        int timeoutForJob = 25

        if (jobName.equals("tools")) {
            out.println("createUnittestGerrit()")

            Map extraEnvVars = [:]
            extraEnvVars.put('CIL_CLI_CONFIG_DIR', '\${WORKSPACE}')

            CilGerritUnitTestJobBuilder gerritUnitTestJobBuilder = new CilGerritUnitTestJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_gerrit_unit_test",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    projectName: projectName,
                    verboseGerritFeedback: verboseGerritFeedback,
                    dslFactory: dslFactory,
                    extraEnvironmentVariables: extraEnvVars,
                    generateGUIconfig: true
                    )
            gerritUnitTestJobBuilder.build()
        } else if (jobName.equals("server")){
            out.println("createUnittestGerrit()")
            CilServerGerritUnitTestJobBuilder gerritUnitTestJobBuilder = new CilServerGerritUnitTestJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_unit_test",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                verboseGerritFeedback: verboseGerritFeedback,
                symlinkWorkspace: this.symlinkWorkspace,
                dslFactory: dslFactory
                )
            gerritUnitTestJobBuilder.build()
        }
        else {
            CilGerritUnitTestJobBuilder gerritUnitTestJobBuilder = new CilGerritUnitTestJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_unit_test",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory,
                timeoutForJob: timeoutForJob
                )
            gerritUnitTestJobBuilder.build()
        }
    }

    protected void createDeploy() {
        boolean generateSoftwareRecord = false
        int timeout = 15
        out.println("createDeploy()")

        if (gerritName.equalsIgnoreCase("cil_client") || gerritName.equalsIgnoreCase("cil_tools") ) {
            timeout = 30
        }

        if (gerritName.contains("client")) {
            generateSoftwareRecord = true
        }
        else if (gerritName.contains("server")) {
            generateSoftwareRecord = true
            timeout = 30

            workspacePath = '/local/jenkins_eforge/\${JOB_NAME}@\${EXECUTOR_NUMBER}'
        }

        Map extraEnvVars = null
        boolean generateGUIconfig = false
        if (jobName.equals("tools")) {
            extraEnvVars = [:]
            extraEnvVars.put('CIL_CLI_CONFIG_DIR', '\${WORKSPACE}')
            generateSoftwareRecord = true
            generateGUIconfig = true
        }

        DeployJobBuilder deployJobBuilder = new CilCreateSoftwareRecordDeploy(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                releaseGoal: releaseGoal,
                releaseDryrunGoal: releaseDryrunGoal,
                jobName: folderName + "/" + jobName + "_deploy",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                generateSoftwareRecord: generateSoftwareRecord,
                timeoutForJob: timeout,
                dslFactory: dslFactory,
                extraEnvironmentVariables: extraEnvVars,
                generateGUIconfig: generateGUIconfig
        )
        deployJobBuilder.build()
    }

    protected void createReleaseDeploy() {
        out.println("createReleaseDeploy()")
        boolean generateSoftwareRecord = false
        boolean generateGUIconfig = false
        def releaseProjects = getReleaseBranches()
        if (gerritName.contains("client") || gerritName.contains("server") || jobName.equals("tools")) {
            generateSoftwareRecord = true
            generateGUIconfig = true
        }

        if (releaseProjects.size() != 0 && releaseProjects != null) {
            if (generateSoftwareRecord) {
                releaseProjects.each {
                    def branchName = it
                    out.println('[INFO] Create release branch for: ' + branchName)
                    DeployJobBuilder releaseBranchDeployJobBuilder = new CilCreateSoftwareRecordDeploy(
                            gerritUser: gerritUser,
                            gerritServer: gerritServer,
                            releaseGoal: releaseGoal,
                            releaseDryrunGoal: releaseDryrunGoal,
                            jobName: folderName + "/release/" + jobName + "_" + branchName.replace('/', '_'),
                            mavenRepositoryPath: mavenRepositoryPath,
                            mavenSettingsFile: mvnSettingFile,
                            gerritName: gerritName,
                            generateSoftwareRecord: generateSoftwareRecord,
                            projectName: projectName,
                            dslFactory: dslFactory,
                            generateGUIconfig: generateGUIconfig
                    )

                    releaseBranchDeployJobBuilder.buildReleaseBranch(branchName)
                }
            } else {
                super.createReleaseDeploy()
            }
        }
    }
}
