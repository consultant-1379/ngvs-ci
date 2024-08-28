package com.ericsson.bss

import com.ericsson.bss.util.GitUtil

import javaposse.jobdsl.dsl.Job

public abstract class AbstractGerritJobBuilder extends AbstractJobBuilder {

    protected String[] gerritTopicPatterns = []

    @Override
    public void initProject(Job job) {
        super.initProject(job)

        job.with {
            properties { priority(JOB_PRIORITY_GERRIT_JOBS) }
        }

        setPublishers()
    }

    protected void setPublishers() {
        job.with {
            publishers {
                if (gerritFeedbackSuccess(verboseGerritFeedback) != null) {
                    flexiblePublish {
                        conditionalAction {
                            condition { status('SUCCESS', 'SUCCESS') }
                            steps { shell(gerritFeedbackSuccess(verboseGerritFeedback)) }
                        }
                    }
                }

                if (gerritFeedbackFail() != null) {
                    flexiblePublish {
                        conditionalAction {
                            condition { status('ABORTED', 'FAILURE') }
                            steps { shell(gerritFeedbackFail()) }
                        }
                    }
                    flexiblePublish {
                        conditionalAction {
                            condition { status('UNSTABLE', 'UNSTABLE') }
                            steps { shell(gerritFeedbackFail()) }
                        }
                    }
                }

                //Workspace on ramdrive needs to be clean up.
                wsCleanup()
            }
        }
    }

    /**
     * Should be override in specific gerrit job
     */
    protected String gerritFeedbackSuccess(boolean verboseGerritFeedback) {
        return null
    }

    /**
     * Should be override in specific gerrit job
     */
    protected String gerritFeedbackFail() {
        return null
    }

    protected String gitInitiateRepository(String repositoryLocation) {
        return "" +
                getShellCommentDescription("Git Initiate Repository") +
                "test -f " + repositoryLocation + "/.git || git init " + repositoryLocation
    }

    protected String gitFetchGerritChange(String repositoryLocation) {
        String cmdFetch = "git -c gc.auto=10000 fetch " + GitUtil.getGitServerUrl(gerritServer) +
                "/\${GERRIT_PROJECT} \${GERRIT_REFSPEC}"

        cmdFetch += addRetry(cmdFetch)
        return "" +
                getShellCommentDescription("Git Fetch Gerrit Change") +
                cmdFetch + "\n" +
                "git reset --hard \${GERRIT_PATCHSET_REVISION}"
    }

    protected void gerritTriggerSilent(String branchName = '.*') {
        job.with {
            triggers {
                gerrit {
                    project(gerritName, 'reg_exp:' + branchName)
                    configure { project ->
                        (project / 'silentMode').setValue('true')
                        /*
                         * TODO: We need to specify the gerrit server to be used, if more than 1 server.
                         * This also putting a dependency on jenkins configuration,
                         * so the gerrit server name is same as we set in dsl.
                         *
                         * Example to specify:
                         * (it / 'serverName').setValue('gerrit.epk.ericsson.se')
                         */

                        if (gerritTopicPatterns.length > 0) {
                            project / gerritProjects / 'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject' << filePaths {
                                for (gerritTopicPattern in gerritTopicPatterns) {
                                    'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.FilePath' {
                                        compareType("ANT")
                                        pattern(gerritTopicPattern)
                                    }
                                }
                            }
                        }
                        project / triggerOnEvents {
                            'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.events.PluginPatchsetCreatedEvent' {
                            }
                            'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.events.PluginDraftPublishedEvent' {
                            }
                        }
                    }
                }
            }
        }
    }

    protected void gerritCodeReviewTriggerSilent(String branchName = '.*') {
        job.with {
            triggers {
                gerrit {
                    events {
                        commentAdded()
                    }
                    project(gerritName, 'reg_exp:' + branchName)
                    configure { project ->
                        if (gerritTopicPatterns.length > 0){
                            project / gerritProjects / 'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject' << filePaths {
                                for (gerritTopicPattern in gerritTopicPatterns) {
                                    'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.FilePath' {
                                        compareType("ANT")
                                        pattern(gerritTopicPattern)
                                    }
                                }
                            }
                        }

                        (project / 'silentMode').setValue('true')
                        project / triggerOnEvents {
                            'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.events.PluginCommentAddedEvent' {
                                verdictCategory('Code-Review')
                                commentAddedTriggerApprovalValue('1')
                            }
                        }
                    }
                }
            }
        }
    }

    protected void getFastPublishers() {
        job.with {
            publishers {
                flexiblePublish {
                    conditionalAction {
                        condition { status('ABORTED', 'FAILURE') }
                        steps { shell(removeOldArtifacts()) }
                    }
                }
            }
        }
    }

    protected String getNotifySetting() {
        return "--notify OWNER "
    }

    protected String addRetry(String cmd) {
        return  " || for i in {1..5}; do " + cmd + "&&break ; sleep=\$((2**\$i+\$RANDOM*10/32767)) ; echo ERROR retrying in \$sleep s ; sleep \$sleep ; done"
    }

    protected String addGerritUser(String feedbackforGerritUser) {
        if (feedbackforGerritUser == null) {
            /* If null then may be on gerrit central, but have not yet setup isUsingLabelForReviews gerrit repo config */
            feedbackforGerritUser = gerritUser
        }
        if (isUsingLabelForReviews()) {
            "GERRIT_USER=\"" + gerritUser + "\"\n"
        }
        else {
            "GERRIT_USER=\"" + feedbackforGerritUser + "\"\n"
        }
    }

    protected boolean isUsingLabelForReviews() {
        String[] readyProjects = [
            "bssf/",
            "cat/",
            "jive-framework/",
            "diameter-base-java/",
        ]
        boolean result = false
        if (gerritServer == Project.GERRIT_CENTRAL_SERVER) {
            readyProjects.each {
                if (gerritName.startsWith(it)) {
                    result = true
                    return true
                }
            }
        }
        return result
    }

    protected String copySitePreview(path, namespace, projectName){
        String sitesPath = System.getProperty("user.home")
        String cmd = """
        SITES_PATH='""" + sitesPath + """'
        # empty the directory we deploy to
        rm -rf \${SITES_PATH}/maven-sites/gerrit/""" + namespace + """/\${GERRIT_PATCHSET_REVISION}

        # create preview directory
        mkdir -p \${SITES_PATH}/maven-sites/gerrit/""" + namespace + """/\${GERRIT_PATCHSET_REVISION}

        # copy to preview directory, hosted on """ + getSitePreviewUrl(namespace, projectName) + """ \${GERRIT_PATCHSET_REVISION}
        cp --parents -r  \$(find . -path '""" + path + """') \${SITES_PATH}/maven-sites/gerrit/""" + namespace + """/\${GERRIT_PATCHSET_REVISION}/
        """
        return getShellCommentDescription("Host preview of site for " + projectName) + cmd.stripIndent()
    }

    protected String getSitePreviewUrl(namespace, projectName){
        String sitePreviewUrl = "https://eta.epk.ericsson.se"
        if (System.getProperty("user.name").toLowerCase().startsWith("kacx")) {
            sitePreviewUrl = "https://" + projectName + ".epk.ericsson.se"
        }
        return sitePreviewUrl + "/maven-sites/gerrit/" + namespace + "/"
    }
}
