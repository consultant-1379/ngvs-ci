package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.AutoAddReviewerJobBuilder
import com.ericsson.bss.job.GerritSonarJobBuilder
import com.ericsson.bss.job.SonarJobBuilder

class Umi extends Project {
    public String projectName = "bssf-umi"
    public String projectGitPrefix = "bssf/umi/"

    private final static boolean ENABLE_CODE_REVIEW_TRIGGER = true

    public Umi() {
        super.projectName = this.projectName
        verboseGerritFeedback = false
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = new ArrayList()

        String output = getGerritforgeProjects()

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains(projectGitPrefix)) {
                repositories.add(repository)
            }
        }

        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        return "umi-" + repository.replace(projectGitPrefix, "")
    }

    @Override
    protected void createForRepository() {
        if (jobName.contains('esa-integration') || jobName.contains('computing.platform')) {
            out.println('[info] settings sdi settings.xml')
            mvnSettingFile = mvnSettingFilePath + 'kascmadm-settings_arm-sdi.xml'
        }
        else {
            mvnSettingFile = mvnSettingFilePath + 'kascmadm-settings_arm-charging.xml'
        }

        super.createForRepository()
    }

    protected void createSonar() {
        boolean generateCoberturaReport = false
        if (jobName.contains('vmmanager')) {
            generateCoberturaReport = true
        }

        out.println("createSonar()")
        SonarJobBuilder sonarJobBuilder = new SonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                generateCoberturaReport: generateCoberturaReport,
                dslFactory: dslFactory
                )

        sonarJobBuilder.build()
    }

    protected void createSonarGerrit() {
        boolean generateCoberturaReport = false
        if (jobName.contains('vmmanager')) {
            generateCoberturaReport = true
        }

        out.println("createSonarGerrit()")
        GerritSonarJobBuilder gerritSonarJobBuilder = new GerritSonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                generateCoberturaReport: generateCoberturaReport,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory
                )

        gerritSonarJobBuilder.build()
    }

    @Override
    protected void createAutoAddReviewer() {
        String[] reviewers = ['"RM UMI Submitters"']
        String[] topicPatterns = ["**"]

        AutoAddReviewerJobBuilder autoAddReviewerJobBuilder = new AutoAddReviewerJobBuilder(
                gerritUser: gerritUser,
                reviewers: reviewers,
                codeReviewTrigger: ENABLE_CODE_REVIEW_TRIGGER,
                gerritTopicPatterns: topicPatterns,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_add_reviewers",
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                dslFactory: dslFactory
        )

        autoAddReviewerJobBuilder.build()
    }
}
