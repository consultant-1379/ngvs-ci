package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.AutoAddReviewerJobBuilder
import com.ericsson.bss.job.DeployJobBuilder
import com.ericsson.bss.job.GerritDependencyTestJobBuilder
import com.ericsson.bss.job.GerritIntegrationTestJobBuilder
import com.ericsson.bss.job.GerritPomCheckJobBuilder
import com.ericsson.bss.job.GerritSiteJobBuilder
import com.ericsson.bss.job.GerritSonarJobBuilder
import com.ericsson.bss.job.MvnGerritUnitTestJobBuilder
import com.ericsson.bss.job.SiteJobBuilder
import com.ericsson.bss.job.SonarJobBuilder
import com.ericsson.bss.job.chargingaccess.ChargingAccessDeployJobBuilder
import com.ericsson.bss.job.chargingaccess.ChargingAccessGerritIntegrationTestJobBuilder
import com.ericsson.bss.job.chargingaccess.ChargingAccessGerritPomCheckJobBuilder
import com.ericsson.bss.job.chargingaccess.ChargingAccessGerritSonarJobBuilder
import com.ericsson.bss.job.chargingaccess.ChargingAccessGerritUnitTestJobBuilder
import com.ericsson.bss.job.chargingaccess.ChargingAccessSonarJobBuilder
import com.ericsson.bss.job.chargingaccess.ChargingAccessTargethostInstallRpmJobBuilder
import com.ericsson.bss.util.GerritUtil

class ChargingAccess extends Project {
    public static final String PROJECT_NAME = "charging.access"
    private static final String REPOSITORY_PREFIX = 'com.ericsson.bss.rm.charging.access'
    private static final int GERRIT_INTEGRATION_TEST_TIMEOUT = 20
    private static final int TIMEOUT_FOR_LONG_RUNNING_JOB = 30

    public ChargingAccess() {
        projectName = PROJECT_NAME
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-charging_access.xml"
        integrationTestRepository = gerritUser + "@" + gerritServer + ":29418/charging/com.ericsson.bss.rm.charging.access.integrationtest"
    }

    @Override
    protected List getRepositories() {
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))
        List<String> repositories = new ArrayList()
        for (repository in output.split(System.lineSeparator())) {
            if (repository.contains(REPOSITORY_PREFIX)) {
                repositories.add(repository)
            }
        }
        repositories.remove("charging/com.ericsson.bss.rm.charging.access.cache")
        repositories.remove("charging/com.ericsson.bss.rm.charging.access.config")
        repositories.remove("charging/com.ericsson.bss.rm.charging.access.nfnt")
        repositories.remove("charging/com.ericsson.bss.rm.charging.access.oam.cm")
        repositories.remove("charging/com.ericsson.bss.rm.charging.access.oam.functiondefinition")
        repositories.remove("charging/com.ericsson.bss.rm.charging.access.oam.health")
        repositories.remove("charging/com.ericsson.bss.rm.charging.access.oam.trace")
        repositories.remove("charging/com.ericsson.bss.rm.charging.access.server")
        repositories.remove("charging/com.ericsson.bss.rm.charging.access.servicelogic")
        repositories.remove("charging/com.ericsson.bss.rm.charging.access.top")
        return repositories
    }

    @Override
    protected List getRepositoriesForGerritJive() {
        List repositories = []
        repositories.add("charging/com.ericsson.bss.rm.charging.access.common")
        repositories.add("charging/com.ericsson.bss.rm.charging.access.configmanager")
        repositories.add("charging/com.ericsson.bss.rm.charging.access.oam")
        repositories.add("charging/com.ericsson.bss.rm.charging.access.oam.fm")
        repositories.add("charging/com.ericsson.bss.rm.charging.access.oam.log")
        repositories.add("charging/com.ericsson.bss.rm.charging.access.oam.pm")
        repositories.add("charging/com.ericsson.bss.rm.charging.access.plugins")
        repositories.add("charging/com.ericsson.bss.rm.charging.access.runtime")
        repositories.add("charging/com.ericsson.bss.rm.charging.access.tools")
        repositories.add("charging/com.ericsson.bss.rm.charging.access.trafficcontroller")

        out.println("Repositories for gerrit jive: " + repositories)

        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        return repository.substring(repository.indexOf(REPOSITORY_PREFIX) + REPOSITORY_PREFIX.length() + 1)
    }

    @Override
    public void create(parent) {
        super.create(parent)
        createInstallRpmJob('Charging/Charging%20Access%20Targethost%20Install%20RPM',
                'suites/installnode/targethost_install_rpm.xml',
                'targethost_install_rpm_${TARGETHOST2}.xml', "access")
    }

    @Override
    protected createJiveTestGerrit() {
        super.createJiveTestGerrit("scripts/gerrit_deploy_to_karaf_then_jive.sh", 45)
    }

    @Override
    protected List getPomCheckRepositoryList(){
        String pomCheckRepositoryList = dslFactory.readFileFromWorkspace('scripts/pomCheckRepositoriesForChargingAccess.groovy')
        return pomCheckRepositoryList.replaceAll("ssh://gerrit.epk.ericsson.se:29418/", "").split("\\n")
    }

    @Override
    protected void createPomAnalysisGerrit() {
        out.println("createPomAnalysisGerrit()")

        if (gerritName in getPomCheckRepositoryList()) {
            GerritPomCheckJobBuilder gerritpomcheckbuilder = new ChargingAccessGerritPomCheckJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_gerrit_check_properties",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    verboseGerritFeedback: verboseGerritFeedback,
                    dslFactory: dslFactory,
                    extraEnvironmentVariables: getExtraEnvironmentVariables(gerritName)
            )

            gerritpomcheckbuilder.build()
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

                GerritSonarJobBuilder gerritSonarJobBuilder = new ChargingAccessGerritSonarJobBuilder(
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
                        timeoutForJob: getDefaultJobTimeout(),
                        generateCoberturaReport: true,
                        extraEnvironmentVariables: getExtraEnvironmentVariables(gerritName)
                )

                gerritSonarJobBuilder.build()
            }
        }
    }

    @Override
    protected void createReleaseSonar() {
        out.println("createReleaseSonar()")

        def releaseProjects = getReleaseBranches()

        if (releaseProjects.size() != 0 && releaseProjects != null) {
            releaseProjects.each {
                def branchName = it
                out.println('[INFO] Create sonar job for: ' + branchName)
                SonarJobBuilder sonarJobBuilder = new ChargingAccessSonarJobBuilder(
                        gerritUser: gerritUser,
                        gerritServer: gerritServer,
                        jobName: folderName + "/release/" + jobName + "_" + branchName.replace('/', '_') + '_sonar',
                        mavenRepositoryPath: mavenRepositoryPath,
                        mavenSettingsFile: mvnSettingFile,
                        gerritName: gerritName,
                        dslFactory: dslFactory,
                        branchName: branchName,
                        generateCoberturaReport: true,
                        generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                        timeoutForJob: getDefaultJobTimeout(),
                        extraEnvironmentVariables: getExtraEnvironmentVariables(gerritName)
                )

                sonarJobBuilder.build()
            }
        }
    }

    @Override
    protected void createUnittestGerrit() {
        out.println("createUnittestGerrit()")
        MvnGerritUnitTestJobBuilder gerritUnitTestJobBuilder = new ChargingAccessGerritUnitTestJobBuilder(
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
                timeoutForJob: getDefaultJobTimeout(),
                extraEnvironmentVariables: getExtraEnvironmentVariables(gerritName)
        )

        gerritUnitTestJobBuilder.build()
    }

    @Override
    protected void createReleaseDeploy() {
        out.println("createReleaseDeploy()")

        def releaseProjects = getReleaseBranches()

        if (releaseProjects.size() != 0 && releaseProjects != null) {
            releaseProjects.each {
                def branchName = it
                out.println('[INFO] Create release branch for: ' + branchName)
                DeployJobBuilder releaseBranchDeployJobBuilder = new ChargingAccessDeployJobBuilder(
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
                        timeoutForJob: getDefaultJobTimeout(),
                        extraEnvironmentVariables: getExtraEnvironmentVariables(gerritName)
                )

                releaseBranchDeployJobBuilder.buildReleaseBranch(branchName)
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
                projectName: projectName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                extraEnvironmentVariables: getExtraEnvironmentVariables(gerritName)
        )

        gerritSiteJobBuilder.build()
    }

    @Override
    protected List getIgnoredRepositoriesForIntegrationTest() {
        return [
                'integrationtest',
                'integration',
                'top'
        ]
    }

    @Override
    protected void createIntegrationTestGerrit() {
        out.println("createIntegrationTestGerrit()")
        boolean ignoreRepository = jobName in getIgnoredRepositoriesForIntegrationTest()
        integrationTestRepository = gerritUser + "@" + gerritServer + ":29418/charging/com.ericsson.bss.rm.charging.access.integrationtest"
        if (!ignoreRepository && integrationTestRepository != null) {
            GerritIntegrationTestJobBuilder gerritIntegrationTestJobBuilder = new ChargingAccessGerritIntegrationTestJobBuilder(
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
                    timeoutForJob: GERRIT_INTEGRATION_TEST_TIMEOUT,
                    extraEnvironmentVariables: getExtraEnvironmentVariables(gerritName, folderName + "/" + jobName + "_gerrit_integration_test")
            )

            gerritIntegrationTestJobBuilder.build()
        }
    }

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
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory,
                extraEnvironmentVariables: getExtraEnvironmentVariables(gerritName)
        )

        gerritDependencyTestJobBuilder.build()
    }

    @Override
    protected void createSonarGerrit() {
        out.println("createSonarGerrit()")
        GerritSonarJobBuilder gerritSonarJobBuilder = new ChargingAccessGerritSonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                timeoutForJob: getDefaultJobTimeout(),
                generateCoberturaReport: true,
                extraEnvironmentVariables: getExtraEnvironmentVariables(gerritName)
        )

        gerritSonarJobBuilder.build()
    }

    @Override
    protected void createInstallRpmJob(String defaultTapasJobPath, String suite = "suites/install_rpm.xml",
                                       String suiteFile = "targethost_install_rpm_${TARGETHOST2}.xml", String tpgVariant = "") {
        out.println("createInstallRpmJob()")
        ChargingAccessTargethostInstallRpmJobBuilder builder = new ChargingAccessTargethostInstallRpmJobBuilder(
                out: out,
                workspacePath: workspacePath,
                suite: suite,
                dslFactory: dslFactory,
                jobName: projectName + '_targethost_rpm_install',
                projectName: projectName,
                gerritName: gerritName,
                gerritServer: '',
                defaultTapasJobPath: defaultTapasJobPath,
                variant: tpgVariant,
                extraEnvironmentVariables: getExtraEnvironmentVariables(gerritName)
        )
        builder.build()
    }

    @Override
    protected void createDeploy() {
        out.println("createDeploy()")

        DeployJobBuilder deployJobBuilder = new ChargingAccessDeployJobBuilder(
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
                timeoutForJob: getDefaultJobTimeout(),
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                extraEnvironmentVariables: getExtraEnvironmentVariables(gerritName)
        )
        deployJobBuilder.build()
    }

    @Override
    protected void createSonar() {
        out.println("createSonar()")

        SonarJobBuilder sonarJobBuilder = new ChargingAccessSonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                dslFactory: dslFactory,
                timeoutForJob: getDefaultJobTimeout(),
                generateCoberturaReport: true,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                extraEnvironmentVariables: getExtraEnvironmentVariables(gerritName, folderName + "/" + jobName + "_sonar")
        )

        sonarJobBuilder.build()
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
                dslFactory: dslFactory,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                timeoutForJob: getDefaultJobTimeout(),
                extraEnvironmentVariables: getExtraEnvironmentVariables(gerritName)
        )

        siteJobBuilder.build()
    }

    @Override
    protected int getDefaultJobTimeout() {
        int timeout = super.getDefaultJobTimeout()

        if (jobName.contains("integrationtest") || jobName.contains("fnt")) {
            timeout = TIMEOUT_FOR_LONG_RUNNING_JOB
        }

        return timeout
    }

    @Override
    protected void createAutoAddReviewer() {
        String[] reviewers = null

        if (jobName.equalsIgnoreCase('bundle') ||
        jobName.equalsIgnoreCase('productiondependencies') ||
        jobName.equalsIgnoreCase('integration')) {
            reviewers = ["\"Charging Buildmaster\""]
        }

        if (jobName.equalsIgnoreCase('integrationtest')) {
            reviewers = ["\"Access IT\""]
        }

        if (jobName.equalsIgnoreCase('oam') ||
        jobName.equalsIgnoreCase('configmanager') ||
        jobName.equalsIgnoreCase('plugins') ||
        jobName.equalsIgnoreCase('runtime') ||
        jobName.equalsIgnoreCase('common') ||
        jobName.equalsIgnoreCase('trafficcontroller') ||
        jobName.equalsIgnoreCase('tools') ||
        jobName.equalsIgnoreCase('oam_fm') ||
        jobName.equalsIgnoreCase('oam_pm') ||
        jobName.equalsIgnoreCase('oam_log')) {
            reviewers = ["\"PDLESS7TRA@pdl.internal.ericsson.com\""]
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

    private static Map getExtraEnvironmentVariables(String gerritName){
        getExtraEnvironmentVariables(gerritName, null)
    }

    private static Map getExtraEnvironmentVariables(String gerritName, String jobName){
        Map<String, String> variables = new HashMap<>()
        if (gerritName.equalsIgnoreCase("charging/com.ericsson.bss.rm.charging.access.integrationtest") ||
        (jobName != null && jobName.contains("_gerrit_integration_test"))) {
            variables.putAll(getLangProperties())
        }

        return variables
    }

    private static getLangProperties() {
        Map<String, String> variables = [:]

        variables.put("LC_ALL", "en_US.UTF-8")
        variables.put("LANG", "en_US.UTF-8")

        return variables
    }

    @Override
    protected void createViews() {
        dslFactory.nestedView(projectName.capitalize()) {
            configure { project ->
                project / defaultView << 'Components'
            }
            views{
                sectionedView('Components') {
                    String linkToGitStatpage = dslFactory.getAt('JENKINS_URL') +
                            'job/' + projectName +
                            '_gitstats/lastSuccessfulBuild/artifact/gitstats_out/index.html'
                    description('Gitstats for ' + projectName +
                            ' can be found <a href="' + linkToGitStatpage +
                            '">here</a>.')
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
                            jobs { regex( projectName + '/.*deploy') }
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
                    jobs { regex( projectName + '.*') }
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
                    statusFilter(StatusFilter.ENABLED)
                    jobs { regex(projectName + '/.*_deploy') }
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
                listView('Site') {
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
                nestedView('Gerrit') {
                    configure { project ->
                        project / defaultView << 'All Gerrit'
                    }
                    views{
                        listView('All Gerrit') {
                            jobs { regex(projectName + '/.*_gerrit_.*') }
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
                        listView('Unit Test') {
                            jobs { regex(projectName + '/.*_gerrit_unit_test') }
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
                        listView('Integration Test') {
                            jobs { regex(projectName + '/.*_gerrit_integration_test') }
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
                        listView('Gerrit Sonar') {
                            jobs { regex(projectName + '/.*_gerrit_sonar') }
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
                        listView('Gerrit Site') {
                            jobs { regex(projectName + '/.*_gerrit_site') }
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
                        listView('Dependency Test') {
                            jobs { regex(projectName + '/.*_gerrit_dependency_test') }
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
    }
}
