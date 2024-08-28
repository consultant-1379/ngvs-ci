package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.GerritDependencyTestJobBuilder
import com.ericsson.bss.job.MvnGerritUnitTestJobBuilder
import com.ericsson.bss.job.PythonSonarRunnerJobBuilder
import com.ericsson.bss.job.jive.JiveDeployJobBuilder
import com.ericsson.bss.job.jive.JiveGerritSonarJobBuilder
import com.ericsson.bss.job.PythonGerritSonarRunnerJobBuilder
import com.ericsson.bss.job.jive.JiveSonarJobBuilder

import javaposse.jobdsl.dsl.Job

class Jive extends Project {
    public String projectName = "jive"
    private Map extraEnvironmentVariables

    public Jive() {
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)
        mvnSettingFile = mvnSettingFilePath + "jive-arm-settings.xml"
        gerritServer = GERRIT_CENTRAL_SERVER
        this.workspacePath = getJenkinsEncodedUrl()
    }

    private String getJenkinsEncodedUrl() {
        // TODO: Remove when jive fixes their paths.
        String encodedUrl = URLEncoder.encode(jenkinsURL, "UTF-8")
        encodedUrl = encodedUrl.replaceAll('%', '_')
        return '/local/' + encodedUrl + '/\${JOB_NAME}_\${EXECUTOR_NUMBER}'
    }

    private addVirtualEnvPath() {
        String virtualEnvPath = '/tmp/\${JOB_NAME}/\${MESOS_EXECUTOR_NUMBER}/'

        this.extraEnvironmentVariables = [:]
        this.extraEnvironmentVariables.put('VENV_TMP', virtualEnvPath)
    }

    @Override
    protected List getRepositories() {
        List repositories = new ArrayList()
        repositories.add("jive-framework/jive-core")
        repositories.add("jive-framework/jive-examples")
        repositories.add("jive-framework/jive-common")
        repositories.add("jive-framework/jive-bom")
        out.println("repositories: " + repositories)
        return repositories
    }

    @Override
    protected void createForRepository() {
        out.println("projectName: " + projectName + ", jobname: " + jobName +
                ", gerritName: " + gerritName)
        createFolders()

        if (gerritName == "jive-framework/jive-core"){
            createJobsForJiveCore()
        }
        if (gerritName == "jive-framework/jive-examples"){
            String[] subProjects = ["jive-domain-examples",
                                    "jive-portal-test",
                                    "jive-quickguide"]
            createJobsForJive(subProjects, false, true, false)
        }

        if (gerritName == "jive-framework/jive-common"){
            String[] subProjects = ["jive-common",
                                    "jive-protocols",
                                    "jive-common-cel",
                                    "jive-common-cil"]
            createJobsForJive(subProjects, true, false, false)
        }

        if (gerritName == "jive-framework/jive-bom"){
            String[] subProjects = ["jive-bom"]
            createJobsForJive(subProjects, true, false, false)
        }
    }

    private void createJobsForJiveCore(){
        // jive-core-all
        String[] includedInJiveCoreAll = ["jive-cli", "jive-core", "jive-core-configuration",
                                          "jive-core-webservice", "jive-core-instrumentation",
                                          "jive-converters", "jive-socket-logging",
                                          "jive-converters-maven-plugin", "jive-core-common"]

        for (int index=0; index < includedInJiveCoreAll.length; index++){
            includedInJiveCoreAll[index] = includedInJiveCoreAll[index] + "/**"
        }
        createSiteGerrit()
        createSite()
        createSonarGerritWithPollingPaths("", includedInJiveCoreAll)
        createGerritUnitTestJobWithPollingPaths("", includedInJiveCoreAll)
        createDeployJobForSubProject("", includedInJiveCoreAll)
        createMvnDependencyTestPollingPaths("", includedInJiveCoreAll)
        createSonarWithPollingPaths("", includedInJiveCoreAll)

        String[] subProjects = ["jive-interfaces", "jive-top"]
        createJobsForJive(subProjects, true, true, true)

        String[] miscProjects = ["jive-diameter-core"]
        createJobsForJive(miscProjects, true, false, false)

        addVirtualEnvPath()

        // for jive-portal
        new PythonSonarRunnerJobBuilder(
                mavenProjectLocation: "jive-portal",
                gitIncludedRegions: "jive-portal/.*",
                gitExcludedRegions: "",
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName +  "_" + "jive-portal" + "_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                generateCoberturaReport: true,
                sonarProfile: "Jive Design Rules",
                extraEnvironmentVariables: this.extraEnvironmentVariables,
                dslFactory: dslFactory
        ).build()

        new PythonGerritSonarRunnerJobBuilder(
                mavenProjectLocation: "jive-portal",
                gerritTopicPatterns: ["jive-portal/**"],
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_" + "jive-portal" + "_gerrit_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                generateCoberturaReport: true,
                gerritName: gerritName,
                verboseGerritFeedback: verboseGerritFeedback,
                sonarProfile: "Jive Design Rules",
                extraEnvironmentVariables: this.extraEnvironmentVariables,
                dslFactory: dslFactory
        ).build()
    }

    private void createJobsForJive(String[] subProjects,
                                   boolean includeDeployJob,
                                   boolean includeSonarJob,
                                   boolean includeDependencyTest){
        for (project in subProjects) {
            String[] pattern = [project + "/**"]
            if (includeSonarJob){
                createSonarWithPollingPaths(project, pattern)
                createSonarGerritWithPollingPaths(project, pattern)
            }

            if (includeDependencyTest){
                createMvnDependencyTestPollingPaths(project, pattern)
            }

            createGerritUnitTestJobWithPollingPaths(project, pattern)
            if (includeDeployJob){
                createDeployJobForSubProject(project,  pattern)
            }
        }
    }

    private void createDeployJobForSubProject(String subProject,
                                              String[] gerritPathPatterns){
        out.println("createDeployJobForSubProject()")
        String jobNameSuffix = ""
        if (subProject) {
            jobNameSuffix = "_" + subProject
        }
        Job deployJob = new JiveDeployJobBuilder(
                mavenProjectLocation: subProject,
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                releaseGoal: releaseGoal,
                releaseDryrunGoal: releaseDryrunGoal,
                jobName: folderName + "/" + jobName + jobNameSuffix + "_deploy",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                dslFactory: dslFactory,
                blameMailList: blameMailList
        ).build()

        jiveGerritTriggerChangeMergedSilent(deployJob, gerritPathPatterns)

        // Add post build steps for certain jobs
        if (subProject == ""){
            out.println("createDeployJobForSubProject() add additional postBuildSteps")
            deployJob.with {
                postBuildSteps {
                    conditionalSteps {
                        condition{
                            status('SUCCESS', 'SUCCESS')
                        }
                        runner('DontRun')
                        steps {
                            downstreamParameterized {
                                trigger('jive_cli_regression_test_candidate')
                                if (subProject == ""){
                                    trigger('jive_core_javadoc')
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void createFolders() {
        out.println("createFolders()")
        folderName = projectName + "/" + jobName
        dslFactory.folder(projectName) {}
        dslFactory.folder(folderName) {}
        jobName = jobName.replace('.', '_')
    }

    private void createSonarWithPollingPaths(String subProject,
                                             String[] gerritTopicPatterns) {
        String jobNameSuffix = ""
        if (subProject) {
            jobNameSuffix = "_" + subProject
        }
        out.println("createSonarWithPollingPaths() " + subProject)
        Job sonarJob = new JiveSonarJobBuilder(
                mavenProjectLocation: subProject,
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + jobNameSuffix + "_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                generateCoberturaReport: false,
                sonarProfile: "Jive Design Rules",
                dslFactory: dslFactory
        ).build()

        jiveGerritTriggerChangeMergedSilent(sonarJob, gerritTopicPatterns)
    }

    private void createGerritUnitTestJobWithPollingPaths(String subProject,
                                         String[] gerritTopicPatterns) {
        String jobNameSuffix = ""
        if (subProject) {
            jobNameSuffix = "_" + subProject
        }
        out.println("createGerritUnitTestJobWithPollingPaths()" + jobNameSuffix)
        new MvnGerritUnitTestJobBuilder(
                mavenProjectLocation: subProject,
                gerritTopicPatterns: gerritTopicPatterns,
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + jobNameSuffix + "_gerrit_unit_test",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory
        ).build()
    }

    private void createSonarGerritWithPollingPaths(String subProject,
                                                   String[] gerritTopicPatterns) {
        String jobNameSuffix = ""
        if (subProject) {
            jobNameSuffix = "_" + subProject
        }
        out.println("createSonarGerritWithPollingPaths()" + jobNameSuffix)
        new JiveGerritSonarJobBuilder(
                mavenProjectLocation: subProject,
                gerritTopicPatterns: gerritTopicPatterns,
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + jobNameSuffix + "_gerrit_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                projectName: projectName,
                mavenSettingsFile: mvnSettingFile,
                generateCoberturaReport: false,
                gerritName: gerritName,
                verboseGerritFeedback: verboseGerritFeedback,
                sonarProfile: "Jive Design Rules",
                dslFactory: dslFactory
        ).build()
    }

    protected void createMvnDependencyTestPollingPaths(String subProject,
                                                       String[] gerritTopicPatterns) {
        String jobNameSuffix = ""
        if (subProject) {
            jobNameSuffix = "_" + subProject
        }
        out.println("createMvnDependencyTestPollingPaths() " + jobNameSuffix)
        new GerritDependencyTestJobBuilder(
                mavenProjectLocation: subProject,
                gerritTopicPatterns: gerritTopicPatterns,
                workspacePath: workspacePath,
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + jobNameSuffix + "_gerrit_dependency_test",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory
        ).build()
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
                    description('Gitstats for '+ projectName +
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
                    jobs { regex( '(.+?-)*?jive(-.+?)*?_.*') }
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
                listView('Jive Classic') {
                    statusFilter(StatusFilter.ENABLED)
                    jobs { regex( '(.+?-)*?jive_core_classic(-.+?)*?_.*') }
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

    protected void jiveGerritTriggerChangeMergedSilent(Job job, String branchName = '.*', String[] gerritPathPatterns) {
        job.with {
            triggers {
                gerrit {
                    events {
                        changeMerged()
                    }
                    project(gerritName, 'reg_exp:' + branchName)
                    configure {
                        (it / 'silentMode').setValue('true')
                    }
                    if (gerritPathPatterns.length > 0) {
                        configure { project ->
                            project / gerritProjects / 'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject' << filePaths {
                                for (gerritPathPattern in gerritPathPatterns) {
                                    'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.FilePath' {
                                        compareType("ANT")
                                        pattern(gerritPathPattern)
                                    }
                                }
                            }
                            (project / 'silentMode').setValue('true')
                        }
                    }
                }
            }
        }
    }
}
