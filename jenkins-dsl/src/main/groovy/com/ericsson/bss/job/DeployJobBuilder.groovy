package com.ericsson.bss.job

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.util.GitUtil

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.helpers.LocalRepositoryLocation

public class DeployJobBuilder extends AbstractJobBuilder {

    public static final String JOB_DESCRIPTION = "<h2>Job that build, package, release and deploy to Artifact Repository Manager " +
    "(<a href='https://wiki.lmera.ericsson.se/wiki/ARM/Home'>ARM</a>)</h2>\n" +
    DETAILED_JOB_CONFIGURATION_DESCRIPTION +
    BSSF_MAVEN_CI_DESCRIPTION +
    "<h3>Overview</h3>" +
    "<p>This job automatically starts when new commits are submitted or can be manually triggered if the user has " +
    "<a href='https://eta.epk.ericsson.se/maven-sites/latest/com.ericsson.bss.rm.charging/parent/site/jenkins.html#jenkins_job_permissions'>" +
    "permission</a>.<br/>\n" +
    "After the artifacts are built and package, the artifacts are deployed to <a href='https://wiki.lmera.ericsson.se/wiki/ARM/Home'>ARM</a>. " +
    "The artifacts are deployed to the location that are specified in the pom file, or inherited from the parent/super pom.</p>\n" +
    "<h4>Release</h4>\n" +
    "<p>The job can also manually be triggered to run a maven " +
    "<a href='https://eta.epk.ericsson.se/maven-sites/latest/com.ericsson.bss.rm.charging/parent/site/component_release.html'>release</a>. Specific " +
    "<a href='https://eta.epk.ericsson.se/maven-sites/latest/com.ericsson.bss.rm.charging/parent/site/jenkins.html#jenkins_job_permissions'>permissions</a>" +
    " is needed to trigger releases</p>\n" +
    "<h4>Problems?</h4>\n" +
    "<ul>\n" +
    "  <li>For issues release to release see " +
    "<a href='https://eta.epk.ericsson.se/wiki/index.php5/Maven_release_troubleshooting_guide'>maven release troubleshooting guide</a>.</li>\n" +
    "  <li>For general maven issues see <a href='" + BSSF_LINK_TO_MAVEN_TROUBLESHOOT + "'>maven troubleshoot guide</a>.</li>\n" +
    "</ul>"

    protected String mavenTarget = "clean deploy"

    protected String releaseGoal
    protected String releaseDryrunGoal
    protected String projectName
    protected String branch = 'master'
    protected String blameMailList
    protected boolean prepareArm2Gask = false

    public Job build() {
        initProject(dslFactory.mavenJob(jobName))
        addDeployConfig()
        return job
    }

    protected void addDeployConfig() {
        job.with {
            description(DSL_DESCRIPTION + JOB_DESCRIPTION)
            addGitRepository(gerritName, branch)
            triggers {
                snapshotDependencies(true)
                if (GitUtil.isLocatedInGitolite(gerritName)) {
                    scm(SCM_POLLING_FREQUENT)
                } else if (gerritServer.equalsIgnoreCase(GitUtil.GERRIT_CENTRAL_SERVER)) {
                    scm('H/5 * * * *')
                } else {
                    scm(SCM_POLLING + '\n# Realtime pushed by the eta_gitscmpoll_trigger job')
                }
            }
            getDeployConfig()
            if (prepareArm2Gask) {
                addPrepareArm2Gask()
            }
            addBlameMail()

            publishers { wsCleanup() }
        }
    }

    protected void addBlameMail() {
        if (gerritName.equalsIgnoreCase("charging/com.ericsson.bss.rm.charging.integrationtest").or(gerritName.equalsIgnoreCase("test_integration"))) {
            String gerritQuery = ''
            if (!projectName.equalsIgnoreCase("charging.core")) {
                gerritQuery = '-gq \'status:merged project:^charging/com' +
                        '.ericsson.bss.rm.' +
                        projectName+'@ branch:master\''
            }
            blamemail(gerritQuery)
        } else {
            addExtendableEmail()
        }
    }

    public void buildReleaseBranch(String branchName) {
        branch = branchName
        initProject(dslFactory.mavenJob(jobName))
        addReleaseConfig()
    }

    protected void addReleaseConfig() {
        job.with {
            description(DSL_DESCRIPTION + JOB_DESCRIPTION)
            addGitRepository(gerritName, branch)
            triggers {
                snapshotDependencies(true)
                scm(SCM_POLLING + '\n# Realtime pushed by the eta_gitscmpoll_trigger job')
            }
            getDeployConfig()

            publishers { wsCleanup() }
        }
    }

    protected getDeployConfig() {
        workspacePath
        Closure preSteps = {
            shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
            shell(removeOldArtifacts())
            shell(gitConfig("\${WORKSPACE}"))
            if (generateGUIconfig) {
                shell(gconfWorkspaceWorkaround())
            }
        }

        job.with {
            preBuildSteps(preSteps)

            Map environmentVariables = getInjectVariables()
            //We may need to change maven version to be used, depending if its master or release branch
            environmentVariables.put('M2_HOME', getMavenHome(branch))
            injectEnv(environmentVariables)

            addMavenConfig()
            if (workspacePath != null && workspacePath != "") {
                customWorkspace(workspacePath)
            }
            addMavenRelease()
            addTimeoutConfig()
        }
    }

    protected void addMavenConfig() {
        //this is required since a method cannot be executed directly in mavenInstallation() step
        String mavenInstallationName = getMavenInstallationName(branch)
        job.with {
            mavenInstallation(mavenInstallationName)

            if (mavenProjectLocation){
                rootPOM(mavenProjectLocation)
            }else {
                rootPOM("pom.xml")
            }

            configure  { project ->
                project / blockTriggerWhenBuilding(false)
            }

            goals(getMvnGoal())
            mavenOpts("\${MAVEN_OPTS}")
            localRepository(LocalRepositoryLocation.LOCAL_TO_WORKSPACE)
            archivingDisabled(true)
            addMavenSettingFile()
        }
    }

    protected String getMvnGoal(){
        String mvnGoal = mavenTarget
        if (profilesToBeUsed != null && profilesToBeUsed != "") {
            mvnGoal += ' -P' + profilesToBeUsed
        }
        if (extraMavenParameters != null && extraMavenParameters != "") {
            mvnGoal += ' ' + extraMavenParameters
        }
        mvnGoal += ' ' + MAVEN_PARAMETERS
        return mvnGoal
    }

    protected void addMavenSettingFile() {
        job.with {
            configure { project ->
                project / settings(class: 'jenkins.mvn.FilePathSettingsProvider') { path("\${MAVEN_SETTINGS}") }
            }
        }
    }

    protected void addMavenRelease() {
        if (!GitUtil.containsWildcards(branch)) {
            job.with {
                wrappers {
                    mavenRelease {
                        releaseGoals(getReleaseGoal())
                        dryRunGoals(getReleaseDryrunGoal())
                        numberOfReleaseBuildsToKeep(-1)
                    }
                }
            }
        }
    }

    protected String getReleaseGoal() {
        String goal = releaseGoal + ' ' + MVN_RELEASE_PARAMETERS

        if (mavenReleaseUser && !GitUtil.isLocatedInGitolite(gerritName)) {
            return '-Dusername=chargingsystem_local ' + goal
        }

        return goal
    }

    protected String getReleaseDryrunGoal() {
        String goal = releaseDryrunGoal + ' ' + MVN_RELEASE_PARAMETERS

        if (mavenReleaseUser && !GitUtil.isLocatedInGitolite(gerritName)) {
            return '-Dusername=chargingsystem_local ' + goal
        }

        return goal
    }

    protected void blamemail(String gerritQuerry) {
        job.with {
            publishers {
                downstreamParameterized {
                    trigger('eta_blame_trigger') {
                        condition("ALWAYS")
                        triggerWithNoParameters(false)
                        parameters {
                            predefinedProps([
                                CURR_BUILD: '${BUILD_NUMBER}',
                                JOB: '${JOB_NAME}',
                                EXTRA_PARAMETERS: '-cc ' + blameMailList + ' -co -o ' + gerritQuerry
                            ])
                        }
                    }
                }
            }
        }
    }

    protected void addPrepareArm2Gask() {
        String buildName = projectName + '_prepare_arm2gask'
        job.with {
            postBuildSteps('SUCCESS') {
                conditionalSteps {
                    condition {
                        and { booleanCondition('\${IS_M2RELEASEBUILD}') }
                                { expression('false', '\${MVN_ISDRYRUN}') }
                    }
                    runner('DontRun')
                    steps {
                        downstreamParameterized {
                            trigger(buildName) {
                                parameters {
                                    predefinedProps([
                                        PRODUCTREPOSITORY: gerritName,
                                        VERSION: '\${MVN_RELEASE_VERSION}'
                                    ])
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
