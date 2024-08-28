package com.ericsson.bss.job

import javaposse.jobdsl.dsl.Job

class GerritSiteJobBuilder extends MvnGerritUnitTestJobBuilder {

    protected String extraMavenOptions = ""

    private String triggerFilePath = '.*src\\/site.*'

    @Override
    public Job build() {
        timeoutForJob = 30
        initProject(dslFactory.freeStyleJob(jobName))
        runParallelThreads = false
        addUnitTestConfig()
        gerritTriggerSilent()
        return job
    }

    @Override
    protected void extraShellSteps() {
        job.with {
            steps {
                shell(removeOldArtifacts())
                shell(mavenBuildCommand())
                shell(super.junitPublisherWorkaround())
                shell(copySitePreview("*target/site", "site", projectName))
            }
        }
    }

    @Override
    protected void gerritTriggerSilent() {

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
                        it / triggerOnEvents {
                            'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.events.PluginPatchsetCreatedEvent' {
                                excludeDrafts('true')
                                excludeNoCodeChange('true')
                            }
                            'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.events.PluginDraftPublishedEvent' {
                            }
                        }

                        if (triggerFilePath != null && triggerFilePath != "" && triggerFilePath != ".*") {
                            (it / 'gerritProjects' / 'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject' / 'filePaths' / 'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.FilePath' {
                                'compareType'('REG_EXP')
                                'pattern'(triggerFilePath)
                            })
                        }
                    }
                }
            }
        }
    }

    protected String tellGerritReviewStarted() {
        return null
    }

    @Override
    protected String mavenBuildCommand() {
        String siteCommand = 'package site \\\n-DskipTests' + extraMavenOptions
        if (projectName.equals('rmca')) {
            super.mavenBuildCommand().replace('install', siteCommand) +
                                                         ' \\\n-Dskip.cdt2.build=true'
        } else {
            super.mavenBuildCommand().replace('install', siteCommand)
        }
    }

    @Override
    protected String gerritFeedbackSuccess(boolean verboseGerritFeedback) {
        String cmd = "" +
                "ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT} " + getNotifySetting() +
                "-m '\"Site preview: " + getSitePreviewUrl("site", projectName) + "'\${GERRIT_PATCHSET_REVISION}' \"' \${GERRIT_PATCHSET_REVISION}"
        cmd += addRetry(cmd)

        return addGerritUser(jenkinsCodeQualityUser) + cmd
    }

    @Override
    protected String gerritFeedbackFail() {
        String cmd = "ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT} " + getNotifySetting() + "-m '\"Site fail to build, '\${BUILD_URL}'\"' " +
                "\${GERRIT_PATCHSET_REVISION}"
        cmd += addRetry(cmd)

        return addGerritUser(jenkinsCodeQualityUser) + cmd
    }

    protected getInjectVariables() {
        def env_list = super.getInjectVariables()

        env_list.put("GRAPHVIZ_DOT", GRAPHVIZ_HOME + "/dot")
        env_list['PATH'] += ":" + GRAPHVIZ_HOME
        return env_list
    }
}
