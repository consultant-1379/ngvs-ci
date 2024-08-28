package com.ericsson.bss.job.cel

import com.ericsson.bss.AbstractGerritTestJobBuilder

import javaposse.jobdsl.dsl.Job

class CelGerritSeleniumJobBuilder extends AbstractGerritTestJobBuilder {

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        timeoutForJob = 45
        gerritTriggerSilent()
        addSeleniumConfig()
        return job
    }

    private void addSeleniumConfig() {
        CelSeleniumHelper helper = new CelSeleniumHelper(this)
        helper.addCommonSeleniumConfig(profilesToBeUsed)
        job.with {
            concurrentBuild()
            steps {
                if (symlinkWorkspace) {
                    shell(symlinkMesosWorkSpace())
                }
                shell(gitInitiateRepository("\${WORKSPACE}"))
                shell(gitConfig("\${WORKSPACE}"))
                shell(getGitCache("\${WORKSPACE}"))
                shell(gitFetchGerritChange())
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
                shell(removeOldArtifacts())
                shell(helper.getSedCommand())
                shell(helper.getBuildCommand())
                shell(helper.getRunBackendCommand())
                shell(helper.getSeleniumCommand(profilesToBeUsed))
                shell(super.junitPublisherWorkaround())
            }
        }
    }

    @Override
    protected void setPublishers() {
        CelSeleniumHelper helper = new CelSeleniumHelper(this)
        helper.addKillBackendCommand()
        job.with {
            publishers {
                archiveJunit('**/target/surefire-reports/')
            }
        }
        super.setPublishers()
    }

    @Override
    protected void gerritTriggerSilent() {
        super.gerritTriggerSilent()
        job.with {
            triggers {
                gerrit {
                    project(gerritName, 'reg_exp:.*')
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
                    }
                }
            }
        }
    }

    @Override
    protected String gerritFeedbackFail() {
        return "GERRIT_USER=\"" + jenkinsIntegrationTestUser + "\"\n" +
                "ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT} " + getNotifySetting() + "-m '\"Selenium tests failed: " +
                "'\${BUILD_URL}'\"' " +
                "-l Code-Review=-1 \${GERRIT_PATCHSET_REVISION}\n"
    }

    @Override
    protected String gerritFeedbackSuccess(boolean verboseGerritFeedback) {
        String cmd = "GERRIT_USER=\"" + jenkinsIntegrationTestUser + "\"\n" +
                "ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT} " + getNotifySetting()

        if (verboseGerritFeedback) {
            cmd += "-m '\"No faults found by Selenium tests. " +
                    "To view report: '\${BUILD_URL}'\"' " +
                    "-l Code-Review=1 \${GERRIT_PATCHSET_REVISION}\n"
        }
        else{
            cmd += "-l Code-Review=0 \${GERRIT_PATCHSET_REVISION}\n"
        }

        return cmd
    }
}
