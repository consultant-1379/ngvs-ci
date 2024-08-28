package com.ericsson.bss.job

import com.ericsson.bss.AbstractGerritJobBuilder
import javaposse.jobdsl.dsl.Job

public class AutoAddReviewerJobBuilder extends AbstractGerritJobBuilder {

    private String gerritName

    protected boolean codeReviewTrigger

    private String[] reviewers

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addAutoAddReviewerConfig()

        if (codeReviewTrigger){
            super.gerritCodeReviewTriggerSilent()
        } else {
            super.gerritTriggerSilent()
        }

        return job
    }

    public void addAutoAddReviewerConfig() {

        job.with {

            String jobDescription = "" +
                    "<h2>Automatically add reviewers for new patch-sets.</h2>"

            description(DSL_DESCRIPTION + jobDescription)

            injectEnv(getInjectVariables())

            steps { shell(getGerritAddReviewsCommand(getReviewers())) }

            addTimeoutConfig()
        }
    }

    private String getGerritAddReviewsCommand(String reviewers) {
        String addReviewerComm = "#!/bin/bash -e\n" +
                                "/usr/bin/ssh -v -p 29418 " + gerritServer + " gerrit set-reviewers " + reviewers + " \$GERRIT_PATCHSET_REVISION\n" +
                                "REVIEWLIST=`echo "+reviewers+" | sed -e 's/-a//g' | sed \"s/'//g\"`\n" +
                                "/usr/bin/ssh -v -p 29418 " + gerritServer + " gerrit review --message \"'Automatically added reviewers: \$REVIEWLIST'\" \$GERRIT_PATCHSET_REVISION"
        if (codeReviewTrigger){
            addReviewerComm = "#!/bin/bash -e\n" +
                            "committer_count=`/usr/bin/ssh -o BatchMode=yes -p 29418 " + gerritServer +
                            " gerrit query \$GERRIT_PATCHSET_REVISION label:Code-Review\\>=1," +
                            "\"\$GERRIT_PATCHSET_UPLOADER_EMAIL\" | grep \"rowCount\" | cut -f2 -d\":\"` \n" +
                            "author_count=`/usr/bin/ssh -o BatchMode=yes -p 29418 " + gerritServer +
                            " gerrit query \$GERRIT_PATCHSET_REVISION label:Code-Review\\>=1," +
                            "\"\$GERRIT_CHANGE_OWNER_EMAIL\" | grep \"rowCount\" | cut -f2 -d\":\"` \n" +
                            "if [[ \$committer_count -eq 1 || \$author_count -eq 1 ]]; then\n" +
                            "  /usr/bin/ssh -v -p 29418 " + gerritServer +
                            " gerrit set-reviewers " + reviewers + " \$GERRIT_PATCHSET_REVISION\n" +
                            "  REVIEWLIST=`echo "+reviewers+" | sed -e 's/-a//g' | sed \"s/'//g\"`\n" +
                            "  /usr/bin/ssh -v -p 29418 " + gerritServer +
                            " gerrit review --message \"'Automatically added reviewers: \$REVIEWLIST'\" \$GERRIT_PATCHSET_REVISION\n" +
                            "fi"
        }
        return addReviewerComm
    }

    private String getReviewers() {
        String gerritReviewerList = ""

        for (int i=0; i < reviewers.length; i++){
            gerritReviewerList += " -a '" +reviewers[i] + "'"
        }
        return gerritReviewerList
    }

    @Override
    protected void setPublishers() {
        job.with { publishers {  wsCleanup()
            } }
    }
}
