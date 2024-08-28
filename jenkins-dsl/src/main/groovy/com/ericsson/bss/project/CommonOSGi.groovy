package com.ericsson.bss.project

import com.ericsson.bss.job.AutoAddReviewerJobBuilder

import com.ericsson.bss.AbstractJobBuilder

import com.ericsson.bss.Project

class CommonOSGi extends Project {
    public String projectName = "common_osgi"

    public CommonOSGi(){
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)

        if (System.getProperty("user.name").equalsIgnoreCase("kascmadm")) {
            gerritUser = new String("chargingsystem_local")
        }
        gerritServer = GERRIT_FORGE_SERVER

        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-common-osgi.xml"
        delimiter = '-'
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = new ArrayList()

        String output = getGerritforgeProjects()

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains('common_osgi-')){
                repositories.add(repository)
            }
        }

        repositories.remove('common_osgi-ftp_client')
        repositories.remove('common_osgi-jmx')
        repositories.remove('common_osgi-rm_common')
        repositories.remove('common_osgi-featurecontrol')

        out.println("repositories: " + repositories)

        return repositories
    }

    protected void createDeploy() {
        out.println("createDeploy()")

        if (jobName.equalsIgnoreCase('vaadin')) {
            String argumentsParameters = '-Darguments="' + AbstractJobBuilder.MVN_SETTINGS + ' ' + AbstractJobBuilder.MVN_REPOSIOTRY + '"'
            releaseGoal = DEFAULT_RELEASE_GOAL + ' ' + argumentsParameters
            releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL + ' ' + argumentsParameters
        } else if (jobName.equalsIgnoreCase('metrics') || jobName.equalsIgnoreCase('functioncontrol')) {
            String argumentsParameters = ' -DpreparationGoals="install" -Darguments="' + AbstractJobBuilder.MVN_SETTINGS + ' ' +
                    AbstractJobBuilder.MVN_REPOSIOTRY + '"'
            releaseGoal = DEFAULT_RELEASE_GOAL + ' ' + argumentsParameters
            releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL + ' ' + argumentsParameters
        }

        super.createDeploy()
    }

    @Override
    protected void createReleaseDeploy() {

        if (jobName.contains('vaadin')) {
            String argumentsParameters = '-Darguments="' + AbstractJobBuilder.MVN_SETTINGS + ' ' + AbstractJobBuilder.MVN_REPOSIOTRY + '"'
            releaseGoal = DEFAULT_RELEASE_GOAL + ' ' + argumentsParameters
            releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL + ' ' + argumentsParameters
        } else if (jobName.contains('metrics') || jobName.equalsIgnoreCase('functioncontrol')) {
            String argumentsParameters = ' -DpreparationGoals="install" -Darguments="' + AbstractJobBuilder.MVN_SETTINGS + ' ' +
                    AbstractJobBuilder.MVN_REPOSIOTRY + '"'
            releaseGoal = DEFAULT_RELEASE_GOAL + ' ' + argumentsParameters
            releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL + ' ' + argumentsParameters
        }

        super.createReleaseDeploy()
    }

    @Override
    protected void createAutoAddReviewer() {
        String[] reviewers = null

        if (jobName.equalsIgnoreCase('security')) {
            reviewers = ["eeriwas", "ecowhar"]
        }
        else if (jobName.equalsIgnoreCase('oam')) {
            reviewers = ["epkroek", "epkrdln"]
        }
        else if (jobName.equalsIgnoreCase('functioncontrol')) {
            reviewers = ["epkroek", "eeriwas"]
        }
        else if (jobName.equalsIgnoreCase('trace')) {
            reviewers = ["epkroek", "ecowhar"]
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
}
