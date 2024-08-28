package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.invoicing.InvoicingUmiTestJobBuilder
import com.ericsson.bss.job.MultiRepositoryReleaseJobBuilder
import com.ericsson.bss.util.GerritUtil
import com.ericsson.bss.AbstractJobBuilder

class Invoicing extends Project {
    private static final int DEFAULT_JOB_TIMEOUT = 30
    private List repositoryList
    public static String projectName = "invoicing"
    private String releaseParametersForIntegrationTest = ' -DpreparationGoals="install" -Darguments="' + AbstractJobBuilder.MVN_SETTINGS + ' ' +
            AbstractJobBuilder.MVN_REPOSIOTRY + '"'

    public Invoicing(){
        super.projectName = this.projectName

        repositoryList = []
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.top')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.serializer')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.common')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.messages')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.oam.auditlog')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.oam.fc')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.oam.cm')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.oam.fm')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.oam.pm')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.oam.sr')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.oam.trace')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.trafficcontroller')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.dataaccess')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.stats')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.services')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.controller')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.interaction')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.flow')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.rest')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.processor')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.cli')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.configuration')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.productiondependencies')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.integration')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.integrationtest')
        repositoryList.add('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.jive')

        featureBranchesToTestAndAnalyze.add("feature/bmspoc")
    }

    @Override
    public void init(parent) {
        super.init(parent)
        super.createCreateClusterJob(true)
        super.createRemoveClusterJob()
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-invoicing.xml"
        integrationTestRepository = gerritUser + "@" + gerritServer + ":29418/invoicing/com.ericsson.bss.rm.invoicing.integrationtest"
        createMultiRepositoryReleaseJob()
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = new ArrayList()
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains('invoicing/')){
                repositories.add(repository)
            }
        }

        repositories.add("eftf/inv")

        repositories.remove("invoicing/com.ericsson.bss.rm.invoicing.core")
        repositories.remove("invoicing/com.ericsson.bss.rm.invoicing.invoicereports")

        return repositories
    }

    @Override
    public void create(parent) {
        super.create(parent)
        super.createUmiTestJob( defaultTapasJobPath: 'Invoicing/UMI%20Invoicing%20Test',
                                suite: 'suites/umi_test.xml',
                                suiteFile:  'umi_test_\${TARGETHOST}.xml',
                                useTwoTargethosts: true,
                                umiTestJobBuilderClass: InvoicingUmiTestJobBuilder,
                                timeoutForJob: 240)
            super.createOvfBuildJob('Invoicing/Build%20Invoicing%20\${__VARIANT__}%20OVF',
                                'suites/build_ovf.xml', 'build_ovf_\${TARGETHOST}.xml',
                                ['controller', 'processor'],
                                ['controller':'INVCONTROLLER', 'INVPROCESSOR':'processor'])
        super.createTargethostInstallJob(
                installNodeName: 'vmx-rminvoicing-010',
                versionLocation: ['https://arm.epk.ericsson.se/artifactory/simple/proj-invoicing-release-local/com/ericsson/bss/INVCONTROLLER,' +
                                 'https://arm.epk.ericsson.se/artifactory/proj-invoicing-release-local/com/ericsson/bss/rm/invoicing/integration/invoicingpackage/invoicingcontrollerpackage/',
                                 'https://arm.epk.ericsson.se/artifactory/simple/proj-invoicing-release-local/com/ericsson/bss/INVPROCESSOR,' +
                                 'https://arm.epk.ericsson.se/artifactory/proj-invoicing-release-local/com/ericsson/bss/rm/invoicing/integration/invoicingpackage/invoicingprocessorpackage/'],
                defaultTapasJobPath: 'INVOICING/Targethost%20Invoicing%20Install',
                useTwoTargethosts: true,
                targethostDescription: ['The machine(s) that should be installed with Invoicing controller if INSTALLTYPE ' +
                    'in [full, controller]. If INSTALLTYPE is processor this is the controller node processor will be configured towards.\n' +
                    'If multiple machines, use "<b>;</b>" to separate them. Ex. vmx123;vmx456',
                    'Host(s) that should be deployed with PROCESSOR OVF.' +
                    'If multiple machines, use "<b>;</b>" to separate them. Ex. vmx123;vmx456'],
                ovfPacName: ['INVCONTROLLER', 'INVPROCESSOR'],
                valuesOfResourceProfiles: ['TeamMachine':[ALLOCATED_CPU_IN_CORE * 4, ALLOCATE_MEMORY_IN_GIGABITE * 16,
                                                          ALLOCATED_CPU_IN_CORE * 8, ALLOCATE_MEMORY_IN_GIGABITE * 16],
                                           'TestSystem':[ALLOCATED_CPU_IN_CORE * 4, ALLOCATE_MEMORY_IN_GIGABITE * 16,
                                                         ALLOCATED_CPU_IN_CORE * 8, ALLOCATE_MEMORY_IN_GIGABITE * 32],
                                           'Default':['', '', '', '']],
                installType: ['full', 'controller', 'processor'],
                useDvFile: true,
                useJiveTests: true,
                jiveMetaData: 'https://arm.epk.ericsson.se/artifactory/simple/proj-invoicing-release-local/com/ericsson/bss/rm/invoicing/jive/invjivetest/',
                useMultipleCils: true)
    }

    @Override
    protected void createFolders() {
        super.createFolders()
        dslFactory.folder(folderName + "/feature") { }
    }

    @Override
    protected void createForRepository() {
        releaseGoal = DEFAULT_RELEASE_GOAL

        if (jobName.equalsIgnoreCase('services')) {
            releaseGoal += ' -Dgoals=deploy'
        }
        else if (jobName.equalsIgnoreCase('integration')) {
            releaseGoal += ' -Dgoals=deploy'
        }

        super.createForRepository()
        super.createFeatureDeploy()
        super.createFeatureSonar()
        super.createFeatureSonarGerrit()
    }

    @Override
    protected void createDeploy() {

        if (jobName.contains('integrationtest')) {
            releaseGoal = DEFAULT_RELEASE_GOAL + ' ' + releaseParametersForIntegrationTest
            releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL + ' ' + releaseParametersForIntegrationTest
        }

        super.createDeploy()
    }

    @Override
    protected int getDefaultJobTimeout() {
        return DEFAULT_JOB_TIMEOUT
    }

    @Override
    protected void createReleaseDeploy() {

        if (jobName.contains('integrationtest')) {
            releaseGoal = DEFAULT_RELEASE_GOAL + ' ' + releaseParametersForIntegrationTest
            releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL + ' ' + releaseParametersForIntegrationTest
        }

        super.createReleaseDeploy()
    }

    @Override
    protected String getJobName(String repository) {
        String currentJobName = super.getJobName(repository)

        if (currentJobName.contains(projectName + '.')) {
            currentJobName = currentJobName.split(projectName + '.')[1]
        }
        else if (repository.equals("eftf/inv")) {
            currentJobName = "eftf-inv"
        }

        return currentJobName
    }

    @Override
    protected List getIgnoredRepositoriesForIntegrationTest() {
        return [
            'bundle',
            'devenv',
            'eftf-inv',
            'integration',
            'integrationtest',
            'test_integration',
            'jive',
            'performance',
            'productiondependencies',
            'top',
            'umi'
        ]
    }

    private void createMultiRepositoryReleaseJob() {
        out.println("createMultiRepositoryReleaseJob()")

        List multiRepoReleaseList = repositoryList.collect()
        multiRepoReleaseList.remove('ssh://gerrit.epk.ericsson.se:29418/invoicing/com.ericsson.bss.rm.invoicing.integrationtest')
        MultiRepositoryReleaseJobBuilder releaseJobBuilder = new MultiRepositoryReleaseJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                projectName: projectName,
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                repositoryList: multiRepoReleaseList,
                buildName: 'invoicing-release',
                releaseRepository: 'proj-invoicing-release-local',
                stagingRepository: 'proj-invoicing-staging-local',
                mail: 'PDLRMINVEX@pdl.internal.ericsson.com',
                dslFactory: dslFactory
                )

        releaseJobBuilder.build()
    }

    @Override
    protected List getPomCheckRepositoryList(){
        List allRepositories = []

        repositoryList.each {
            allRepositories.add(it.toString().replaceAll("ssh://gerrit.epk.ericsson.se:29418/", ""))
        }

        return allRepositories
    }
}
