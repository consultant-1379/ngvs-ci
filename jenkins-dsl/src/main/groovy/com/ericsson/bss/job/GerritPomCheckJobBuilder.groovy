package com.ericsson.bss.job

import com.ericsson.bss.AbstractGerritJobBuilder

import javaposse.jobdsl.dsl.Job

public class GerritPomCheckJobBuilder extends AbstractGerritJobBuilder {

    protected String jenkinsPomValidator = "jenkins-pom-validator"

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))

        addGerritPomCheckConfig()
        super.gerritTriggerSilent()
        return job
    }

    private void addGerritPomCheckConfig() {
        job.with {

            String jobDescription = "<h2>Check pom files for unused properties</h2>" +
                    "<p>If a pom file has a unused property, it will be reported to the related gerrit review.</p>"

            description(DSL_DESCRIPTION + jobDescription)

            concurrentBuild()

            injectEnv(getInjectVariables())

            addTimeoutConfig()
        }

        addJobs()
    }

    protected void addJobs() {
        job.with {
            steps {
                shell(gitInitiateRepository("\${WORKSPACE}"))
                shell(gitConfig("\${WORKSPACE}"))
                shell(getGitCache("\${WORKSPACE}"))
                shell(gitFetchGerritChange())
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
                shell(POM_ANALYSIS_LOG_OUTPUT)
                shell(getShellCommentDescription('Scan changed pom files to find unused properties') +
                        dslFactory.readFileFromWorkspace('scripts/findUnusedPomProperties.sh'))
            }
        }
    }

    protected static final String POM_ANALYSIS_LOG_OUTPUT = "echo \"-------------------------------\" >> console_out.txt\n" +
    "echo \"-                             -\" >> console_out.txt\n" +
    "echo \"-  Pom file properties check  -\" >> console_out.txt\n" +
    "echo \"-                             -\" >> console_out.txt\n" +
    "echo \"-------------------------------\" >> console_out.txt\n" +
    ""

    @Override
    protected void setPublishers() {
        job.with {
            publishers {
                archiveArtifacts('console_out.txt')
                textFinder('Unused property', 'console_out.txt', false, false, true)
                flexiblePublish {
                    conditionalAction {
                        condition { alwaysRun() }
                        steps {
                            shell('cat console_out.txt')
                        }
                    }
                }
            }
        }
        super.setPublishers()
    }

    @Override
    protected String gerritFeedbackFail() {
        String reviewCmd = "ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT} " + getNotifySetting() +  "-m '\"See unused pom properties related to this commit\n" +
                "'\${BUILD_URL}'artifact/console_out.txt\"' " +
                "-l Code-Review=-1 \${GERRIT_PATCHSET_REVISION}"

        reviewCmd += addRetry(reviewCmd)

        return "GERRIT_USER=\"" + jenkinsPomValidator + "\"\n" +
                "if [ -f \"console_out.txt\" ]; then \n" +
                "  echo \"See unused pom properties related to this commit: " +
                "\${BUILD_URL}artifact/console_out.txt\"\n" +
                "  " + reviewCmd + "\n" +
                "fi"
    }

    @Override
    protected String gerritFeedbackSuccess(boolean verboseGerritFeedback) {
        String cmd = "ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT} " + getNotifySetting() +
                " -l Code-Review=0 \${GERRIT_PATCHSET_REVISION}"
        cmd += addRetry(cmd)

        return "GERRIT_USER=\"" + jenkinsPomValidator + "\"\n" + cmd
    }
}
