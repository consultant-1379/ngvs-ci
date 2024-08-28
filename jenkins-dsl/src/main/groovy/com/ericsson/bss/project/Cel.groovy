package com.ericsson.bss.project

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.Project
import com.ericsson.bss.job.CreateBranchJobBuilder
import com.ericsson.bss.job.DeployJobBuilder
import com.ericsson.bss.job.GerritDependencyTestJobBuilder
import com.ericsson.bss.job.GerritSiteJobBuilder
import com.ericsson.bss.job.GerritSonarJobBuilder
import com.ericsson.bss.job.SiteJobBuilder
import com.ericsson.bss.job.SonarJobBuilder
import com.ericsson.bss.job.cel.CelDeployJobBuilder
import com.ericsson.bss.job.cel.CelGerritSeleniumJobBuilder
import com.ericsson.bss.job.cel.CelGerritSonarJobBuilder
import com.ericsson.bss.job.cel.CelGerritUnitTestJobBuilder
import com.ericsson.bss.job.cel.CelReleasePrepareJobBuilder
import com.ericsson.bss.job.cel.CelSeleniumJobBuilder
import com.ericsson.bss.job.cel.CelSonarJobBuilder

class Cel extends Project {

    public static String projectName = 'cel'

    private static final String GUI_PROFILE = 'gui'
    private static final String SELENIUM_PROFILE = 'selenium'
    private static final String SELENIUM_PROFILE_WITHOUT_CUCUMBER = SELENIUM_PROFILE + ',!cucumber'
    private static final String SELENIUM_CUCUMBER_PROFILE = 'cucumber'
    private static final String SELENIUM_JIVE_PROFILE = 'jive'
    private static final String EXTRA_RELEASE_PARAMETERS = ' -P' + GUI_PROFILE + ',' + SELENIUM_PROFILE_WITHOUT_CUCUMBER +
            ' -DuseReleaseProfile=false -DpreparationGoals="install" -Darguments="-P' + GUI_PROFILE + ',' + SELENIUM_PROFILE_WITHOUT_CUCUMBER +
            ' -Dorg.ops4j.pax.url.mvn.settings=\${MAVEN_SETTINGS} -Dorg.ops4j.pax.url.mvn.localRepository=\${MAVEN_REPOSITORY} ' +
            '--settings \${MAVEN_SETTINGS} -Dmaven.repo.local=\${MAVEN_REPOSITORY}"'
    private static final String RELEASE_GOAL = DEFAULT_RELEASE_GOAL + ' -Dgoals=deploy' + EXTRA_RELEASE_PARAMETERS
    private static final String RELEASE_DRYRUN_GOAL = DEFAULT_DRYRUN_RELEASE_GOAL + EXTRA_RELEASE_PARAMETERS

    private Map extraEnvironmentVariables
    private int timeout = 45
    private int sonarTimeout = 45
    private int deployTimeout = 90
    private boolean generateGUIconfig = true
    private boolean symlinkWorkspace = true

    public Cel(){
        super.projectName = this.projectName

        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-cel.xml"

        overrideJvmOptions()
    }

    @Override
    public void init(parent) {
        super.init(parent)
    }

    @Override
    protected List getRepositories() {
        List repositories = new ArrayList()
        repositories.add("cel/cel")

        out.println("repositories: " + repositories)
        return repositories
    }

    @Override
    protected String getJobName(String repository) {
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
    }

    @Override
    protected void createForRepository() {
        super.createForRepository()
        createSelenium()
        createSeleniumGerrit()
    }

    @Override
    protected void createSonar() {
        out.println("createSonar()")
        overrideJvmOptions()
        SonarJobBuilder sonarJobBuilder = new CelSonarJobBuilder(
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
        CelSeleniumJobBuilder celSeleniumJobBuilder = new CelSeleniumJobBuilder(
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

        celSeleniumJobBuilder.build()
    }

    private void createSeleniumGerrit() {
        out.println("createSeleniumGerrit()")
        CelGerritSeleniumJobBuilder celGerritSeleniumJobBuilder = new CelGerritSeleniumJobBuilder(
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

        celGerritSeleniumJobBuilder.build()
    }

    @Override
    protected void createUnittestGerrit() {
        out.println("createUnittestGerrit()")
        this.extraEnvironmentVariables.put('MALLOC_ARENA_MAX', '2')
        CelGerritUnitTestJobBuilder celGerritUnitTestJobBuilder = new CelGerritUnitTestJobBuilder(
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
                dslFactory: dslFactory
                )

        celGerritUnitTestJobBuilder.build()
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
        GerritSonarJobBuilder gerritSonarJobBuilder = new CelGerritSonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_sonar",
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

        gerritSonarJobBuilder.build()
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

        overrideJvmOptions()

        String releaseGoalDeploy = RELEASE_GOAL + ' -DpublishToJivePortal=true'

        DeployJobBuilder deployJobBuilder = new CelDeployJobBuilder(
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
                profilesToBeUsed: GUI_PROFILE + ',' + SELENIUM_PROFILE_WITHOUT_CUCUMBER,
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
                DeployJobBuilder releaseBranchDeployJobBuilder = new CelDeployJobBuilder(
                        gerritUser: gerritUser,
                        gerritServer: gerritServer,
                        releaseGoal: RELEASE_GOAL,
                        releaseDryrunGoal: RELEASE_DRYRUN_GOAL,
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

                releaseBranchDeployJobBuilder.buildReleaseBranch(branchName)
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

    private overrideJvmOptions() {
        this.extraEnvironmentVariables = [:]
        this.extraEnvironmentVariables.put('JAVA_TOOL_OPTIONS', AbstractJobBuilder.JAVA_TOOL_OPTIONS + ' -DOSGI_PORT_OVERRIDE=\${ALLOCATED_PORT}')
        this.extraEnvironmentVariables.put('MAVEN_OPTS', AbstractJobBuilder.MAVEN_OPTS  + ' -DOSGI_PORT_OVERRIDE=\${ALLOCATED_PORT}')
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
        CelReleasePrepareJobBuilder celRleasePrepareJobBuilder = new CelReleasePrepareJobBuilder(
                workspacePath: workspacePath,
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
                dslFactory: dslFactory,
                )

        celRleasePrepareJobBuilder.build()
    }
}
