package com.ericsson.bss.job

import com.ericsson.bss.AbstractGerritJobBuilder

import javaposse.jobdsl.dsl.Job

public class GerritDependencyTestJobBuilder extends AbstractGerritJobBuilder {

    protected String projectName

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addMavenDependencyTestConfig()
        super.gerritTriggerSilent()
        return job
    }

    public void addMavenDependencyTestConfig() {
        job.with {

            String jobDescription = "" +
                    "<h2>This job checks for used undeclared dependencies.</h2>"

            description(DSL_DESCRIPTION + jobDescription)

            concurrentBuild()

            steps {
                shell(gitInitiateRepository("\${WORKSPACE}"))
                shell(gitConfig("\${WORKSPACE}"))
                shell(getGitCache("\${WORKSPACE}"))
                shell(gitFetchGerritChange())
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))

                shell(mavenDependencyAnalyzeCommand())
            }

            injectEnv(getInjectVariables())
            addTimeoutConfig()
        }
    }

    @Override
    protected void setPublishers() {
        job.with {
            publishers {
                textFinder('Used undeclared dependencies found', '', true, false, true)
            }
        }
        super.setPublishers()
    }

    @Override
    protected String gerritFeedbackFail() {
        String reviewCmd = "ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                           " gerrit review --project \${GERRIT_PROJECT} " + getNotifySetting() + "-m '\"DependencyTest Unstable," +
                           " '\${BUILD_URL}' '\${HELP_LINK}'\"' \${GERRIT_PATCHSET_REVISION}"
        reviewCmd += addRetry(reviewCmd)
        return addGerritUser(jenkinsCodeQualityUser) +
                "HELP_LINK=\"Used undeclared dependencies found, Use mvn dependency:analyze for full list.\"\n" + reviewCmd
    }

    protected String mavenDependencyAnalyzeCommand() {
        String mavenSubProjectCmd = ""
        if (mavenProjectLocation){
            mavenSubProjectCmd = " -f " + mavenProjectLocation
        }

        String cmd = "" +
                getShellCommentDescription("Maven dependency command") +
                "mvn " + mavenSubProjectCmd + " \\\n" +
                "dependency:analyze \\\n"

        if (runParallelThreads) {
            cmd +=  "-DparallelTests -T 4 \\\n"
        } else {
            cmd += '-Pgui \\\n'
        }
        cmd += "\\\n" +
                "-Dsurefire.useFile=false \\\n" +
                "-DoutputXML=true \\\n" +
                getMavenGeneralBuildParameters() + " \\\n" +
                "| sed -n -e'/Building/p' -e '/Used undeclared/p' -e '/<dependency>/,/<\\/dependency>/p'"

        return cmd
    }
}
