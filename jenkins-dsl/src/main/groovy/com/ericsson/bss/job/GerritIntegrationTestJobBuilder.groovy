package com.ericsson.bss.job

import com.ericsson.bss.AbstractGerritTestJobBuilder

import javaposse.jobdsl.dsl.Job

public class GerritIntegrationTestJobBuilder extends AbstractGerritTestJobBuilder {

    protected String projectName
    protected String integrationTestRepository

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))

        addIntegrationTestConfig()
        gerritTriggerSilent()

        addThrottleConcurrentConfig()
        return job
    }

    protected void addIntegrationTestConfig() {
        job.with {

            String jobDescription = "" +
                    "<h2>Runs Integration test for repository.</h2>" +
                    "<p>This job will run all integration test against the commit in gerrit.</p>\n"  +
                    BSSF_MAVEN_CI_DESCRIPTION

            description(DSL_DESCRIPTION + jobDescription)
            concurrentBuild()
            addBuildSteps()
            injectEnv(getInjectVariables())
            addTimeoutConfig()
        }
    }

    protected void addBuildSteps() {
        job.with {
            steps {
                /* Init current repository to build */
                if (verboseGerritFeedback) {
                    shell(tellGerritReviewStarted())
                }
                shell(gitInitiateRepository("\${WORKSPACE}"))
                shell(gitConfig("\${WORKSPACE}"))
                shell(getGitCache("\${WORKSPACE}"))
                shell(gitFetchGerritChange())
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))

                /* Init IntegrationTest */
                String test_directory = getTestsDirectory()
                shell(gitInitiateIntegrationTestRepository(test_directory))
                shell(cleanUpWorkspaceMesos(test_directory))
                shell(gitConfig(test_directory))
                shell(getGitCache(test_directory))
                shell(gitFetchGerritIntegrationTestChange(test_directory))

                if (generateGUIconfig) {
                    shell(gconfWorkspaceWorkaround())
                }

                shell(mavenCommandToBuildReposiotry())
                shell(OUTPUT_IT_HAS_STARTED)
                shell(mavenCommandToBuildIntegrationTestRepository(test_directory))
                shell(super.junitPublisherWorkaround())
            }
        }
    }

    protected String getTestsDirectory() {
        return ".integrationtest"
    }

    @Override
    protected void gerritTriggerSilent() {
        job.with {
            triggers {
                gerrit {
                    project(gerritName, 'reg_exp:^(?!.*release).*')
                    configure {
                        (it / 'silentMode').setValue('true')
                        /*
                         * TODO: We need to specify the gerrit server to be used, if more than 1 server.
                         * This also putting a dependency on jenkins configuration,
                         * so the gerrit server name is same as we set in dsl.
                         *
                         * Example to specify:
                         * (it / 'serverName').setValue('gerrit.epk.ericsson.se')
                         */
                        it / triggerOnEvents {
                            'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.events.PluginPatchsetCreatedEvent' {
                                excludeDrafts('true')
                                excludeNoCodeChange('true')
                            }
                            'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.events.PluginDraftPublishedEvent' {
                            }
                        }
                    }
                }
            }
        }
    }

    protected String tellGerritReviewStarted() {
        String reviewCmd ="ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " +
                gerritServer + " gerrit review --project \${GERRIT_PROJECT}  " + getNotifySetting() +
                "-m \'\"Integration tests started, \'\${BUILD_URL}\' (-1 is always set when the job start" +
                ")\"\' " + addGerritLabel(-1) + " \${GERRIT_PATCHSET_REVISION}"

        return getShellCommentDescription("Gerrit review started") + addGerritUser(jenkinsIntegrationTestUser) + reviewCmd + addRetry(reviewCmd)
    }

    protected String gitInitiateIntegrationTestRepository(String testDirectory) {
        return gitInitiateIntegrationTestRepository(integrationTestRepository, testDirectory)
    }

    protected String gitInitiateIntegrationTestRepository(String integrationTestRepositoryName, String testDirectory) {
        return "" +
                getShellCommentDescription("Git Initiate Integrationtest Repository") +
                "test -d " + testDirectory + " || git clone --reference \${GIT_CLONE_CACHE} ssh://" +
                integrationTestRepositoryName + " " + testDirectory
    }

    protected String gitFetchGerritIntegrationTestChange(String testDirectory) {
        return "" +
                getShellCommentDescription("Git Fetch Integrationtest") +
                "cd " + testDirectory + "\n" +
                "git fetch\n" +
                "git reset --hard origin/master"
    }

    protected String mavenCommandToBuildReposiotry(){
        String cmd = getShellCommentDescription("Maven command to build repository") +
                "mvn \\\n" +
                "clean install \\\n"
        if (!generateGUIconfig) {
            cmd += "-T2C -DtestForkCount=2C \\\n"
        }
        if (extraMavenParameters != null && extraMavenParameters != "") {
            cmd += ' ' + extraMavenParameters + ' '
        }
        cmd += "-Dsurefire.useFile=false \\\n" +
                "-DskipTests \\\n" +
                getMavenGeneralBuildParameters()

        return cmd
    }

    protected static final String OUTPUT_IT_HAS_STARTED = "/bin/sh\n" +
    "echo \"-------------------------------------\"\n" +
    "echo \"-                                   -\"\n" +
    "echo \"- Integration tests                 -\"\n" +
    "echo \"-                                   -\"\n" +
    "echo \"-------------------------------------\"\n"

    protected String mavenCommandToBuildIntegrationTestRepository(String testDirectory) {
        String cmd = getShellCommentDescription("Maven command to build and run Integration Test repository") +
                "mvn \\\n" +
                "-f " + testDirectory + "/pom.xml \\\n" +
                "clean install \\\n" +
                "-DtestForkCount=1 \\\n"
        if (extraMavenParameters != null && extraMavenParameters != "") {
            cmd += ' ' + extraMavenParameters + ' '
        }
        cmd += "-Dsurefire.useFile=false \\\n" +
                "-DfailIfNoTests=false \\\n" +
                getMavenGeneralBuildParameters()
        return cmd
    }

    @Override
    protected void setPublishers() {
        job.with {
            publishers { archiveJunit('**/surefire-reports/*.xml') }
        }
        super.setPublishers()
    }

    @Override
    protected String gerritFeedbackSuccess(boolean verboseGerritFeedback) {
        String reviewCmd = "" +
                "ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT} " + getNotifySetting()

        if (verboseGerritFeedback) {
            reviewCmd += "" +
                    " -m '\"IntegrationTest SUCCESSFUL," +
                    " '\${BUILD_URL}'\"' " + addGerritLabel(1) + " \${GERRIT_PATCHSET_REVISION}"
        }
        else {
            reviewCmd += addGerritLabel(0) + " \${GERRIT_PATCHSET_REVISION}"
        }

        return addGerritUser(jenkinsIntegrationTestUser) + reviewCmd + addRetry(reviewCmd)
    }

    @Override
    protected String gerritFeedbackFail() {
        def integrationTestReviewHelp = "" +
                "HELP_LINK=\"Please go to https://eta.epk.ericsson.se/maven-sites/latest/com.ericsson.bss.rm." +
                getProjectName() + "/parent/integrationtest/errors.html to find out if you introduced the error or if it is an " +
                "existing error in the IntegrationTest.\"\n"

        def reviewCmd = "ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT} " + getNotifySetting() + "-m '\"IntegrationTest FAILED, '\${BUILD_URL}'\n" +
                "'\${HELP_LINK}'\"' " + addGerritLabel(-1) + " \${GERRIT_PATCHSET_REVISION}"

        return addGerritUser(jenkinsIntegrationTestUser) + integrationTestReviewHelp + reviewCmd + addRetry(reviewCmd)
    }

    private String addGerritLabel(int feedbackValue) {
        if (isUsingLabelForReviews()) {
            "--label Integration-Test=" + feedbackValue
        }
        else {
            "--verified " + feedbackValue
        }
    }

    protected String getProjectName() {
        def project = projectName
        if (projectName == "charging.core") {
            project = "charging"
        }
        return project
    }

    public void addAdditionalIntegrationTestConfig(String secondIntegrationTestRepository) {
        job.with {
            concurrentBuild()
            steps {
                String testDirectory = ".integrationtest2"
                shell(gitInitiateIntegrationTestRepository(secondIntegrationTestRepository, testDirectory))
                shell(cleanUpWorkspaceMesos(testDirectory))
                shell(gitConfig(testDirectory))
                shell(getGitCache(testDirectory))
                shell(gitFetchGerritIntegrationTestChange(testDirectory))
                shell(mavenCommandToBuildIntegrationTestRepository(testDirectory))
            }
        }
    }

    private addThrottleConcurrentConfig(){
        // Add throttle concurrent because the timeout increase to 5 hours
        job.with {
            throttleConcurrentBuilds {
                maxTotal(20)
            }
        }
    }
}
