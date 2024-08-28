package com.ericsson.bss.job

import com.ericsson.bss.AbstractGerritTestJobBuilder

import javaposse.jobdsl.dsl.Job

public class GerritUnitTestJobBuilder extends AbstractGerritTestJobBuilder {
    protected Map envList

    public Job build(params) {

        String branchName = ".*"
        if (null != params && null != params.branchName) {
            branchName = params.branchName
        }

        initProject(dslFactory.freeStyleJob(jobName))
        addUnitTestConfig()
        super.gerritTriggerSilent(branchName)

        return job
    }

    protected void setEnvVariables() {
        envList = getInjectVariables()
    }

    protected void addUnitTestConfig() {
        setEnvVariables()

        job.with {

            String jobDescription = "<h2>Runs all test in the repository.</h2>\n"  +
                    BSSF_MAVEN_CI_DESCRIPTION

            description(DSL_DESCRIPTION + jobDescription)

            concurrentBuild()

            steps {
                if (verboseGerritFeedback && tellGerritReviewStarted() != null) {
                    shell(tellGerritReviewStarted())
                }
                if (symlinkWorkspace) {
                    shell(symlinkMesosWorkSpace())
                }
                shell(gitInitiateRepository("\${WORKSPACE}"))
                shell(gitConfig("\${WORKSPACE}"))
                shell(getGitCache("\${WORKSPACE}"))
                shell(gitFetchGerritChange())
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
            }

            injectEnv(envList)

            addTimeoutConfig()
        }

        extraShellSteps()
    }

    protected void extraShellSteps() {
    }

    protected String tellGerritReviewStarted() {
        String reviewCmd = "ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT} " + getNotifySetting() + "-m \'\"Unit tests started, \'\${BUILD_URL}\' " +
                "(-1 is always set when the job start)\"\' " + addGerritLabel(-1) + " \${GERRIT_PATCHSET_REVISION}"

        return getShellCommentDescription("Gerrit review started") + addGerritUser(jenkinsUnitTestUser) + reviewCmd + addRetry(reviewCmd)
    }

    @Override
    protected String gerritFeedbackSuccess(boolean verboseGerritFeedback) {
        String reviewCmd = "ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT} " + getNotifySetting()

        if (verboseGerritFeedback) {
            reviewCmd += "" +
                    " -m '\"UnitTest SUCCESSFUL," +
                    " '\${BUILD_URL}'\"' " + addGerritLabel(1) + " \${GERRIT_PATCHSET_REVISION}"
        }
        else {
            reviewCmd += "" +
                    " " + addGerritLabel(0) + " \${GERRIT_PATCHSET_REVISION}"
        }

        return addGerritUser(jenkinsUnitTestUser) + reviewCmd + addRetry(reviewCmd)
    }

    @Override
    protected String gerritFeedbackFail() {
        String reviewCmd = "ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT} " + getNotifySetting() + "-m '\"UnitTest FAILED, '\${BUILD_URL}'\"' " +
                addGerritLabel(-1) + " \${GERRIT_PATCHSET_REVISION}"

        return addGerritUser(jenkinsUnitTestUser) + reviewCmd + addRetry(reviewCmd)
    }

    private String addGerritLabel(int feedbackValue) {
        if (isUsingLabelForReviews()) {
            "--label Unit-Test=" + feedbackValue
        }
        else {
            "--verified " + feedbackValue
        }
    }
}
