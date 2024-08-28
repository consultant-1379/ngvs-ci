package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.AutoAddReviewerJobBuilder
import com.ericsson.bss.job.GerritSiteJobBuilder
import com.ericsson.bss.util.GitUtil

class BssFoundation extends Project {
    public String projectName = "bss_foundation"

    public BssFoundation() {
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)

        gerritServer = GERRIT_CENTRAL_SERVER
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-charging.xml"
    }

    @Override
    protected List getRepositories() {
        List repositories = new ArrayList()

        repositories.add("bssf/com.ericsson.bss.top")
        repositories.add("bssf/com.ericsson.bss.common.ecim")
        repositories.add("bssf/com.ericsson.bss.common.schemas")
        repositories.add("bssf/com.ericsson.bss.skin")

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        if (GitUtil.isLocatedInGitolite(repository)) {
            return repository[(repository.lastIndexOf('.') + 1)..-1]
        }
        else {
            //Want to use the existing job name layout as for gerritforge
            return repository.replace('bssf/com.ericsson.bss.', '').replace('.', '_')
        }
    }

    @Override
    protected void createAutoAddReviewer() {

        String[] reviewers = null
        switch (jobName.toLowerCase()) {
            case 'common_schemas':
                reviewers = ["\"BSSF common.schemas Auto Reviewers\""]
                break
        }

        if (reviewers != null) {
            AutoAddReviewerJobBuilder autoAddReviewerJobBuilder = new AutoAddReviewerJobBuilder(
                    gerritUser: gerritUser,
                    reviewers: reviewers,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_gerrit_add_reviewers",
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    dslFactory: dslFactory
            )

            autoAddReviewerJobBuilder.build()
        }
    }

    @Override
    protected void createSiteGerrit() {
        if (jobName == 'skin') {

            out.println("createSiteGerrit()")
            GerritSiteJobBuilder gerritSiteJobBuilder = new GerritSiteJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_gerrit_site",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    projectName: projectName,
                    verboseGerritFeedback: verboseGerritFeedback,
                    dslFactory: dslFactory,
                    generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                    timeoutForJob: getDefaultJobTimeout(),
                    triggerFilePath: ".*"
            )

            gerritSiteJobBuilder.build()
        }
        else {
            super.createSiteGerrit()
        }
    }
}
