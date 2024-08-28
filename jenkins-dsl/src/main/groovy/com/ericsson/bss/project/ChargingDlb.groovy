package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.AutoAddReviewerJobBuilder
import com.ericsson.bss.job.charging.ChargingGerritSonarJobBuilder

class ChargingDlb extends Project {
    public String projectName = "charging.dlb"

    public ChargingDlb(){
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-charging.xml"
        String releaseParameters = ' -DpreparationGoals="install" -Darguments="-Dorg.ops4j.pax.url.mvn.settings=\${MAVEN_SETTINGS}' +
                ' -Dorg.ops4j.pax.url.mvn.localRepository=\${MAVEN_REPOSITORY} --settings \${MAVEN_SETTINGS} -Dmaven.repo.local=\${MAVEN_REPOSITORY}"'
        releaseGoal = DEFAULT_RELEASE_GOAL + ' -Dgoals=deploy' + releaseParameters
        releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL + releaseParameters
    }

    @Override
    protected List getRepositories() {
        List repositories = new ArrayList()

        repositories.add("charging/com.ericsson.bss.rm.charging.dlb")

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        String currentJobName = super.getJobName(repository)

        currentJobName = currentJobName.replace('com.ericsson.bss.rm.', '')

        return currentJobName
    }

    @Override
    public void create(parent) {
        super.create(parent)
        super.createUmiTestJob( defaultTapasJobPath: 'Charging/UMI%20Charging%20DLB%20Test',
                                suite: 'suites/installnode/dlb_umi_test.xml',
                                suiteFile: 'umi_test_\${TARGETHOST}.xml')
    }
    @Override
    protected void createAutoAddReviewer() {
        AutoAddReviewerJobBuilder autoAddReviewerJobBuilder = new AutoAddReviewerJobBuilder(
                gerritUser: gerritUser,
                reviewers: ["CFT10"],
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_add_reviewers",
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                dslFactory: dslFactory
                )

        autoAddReviewerJobBuilder.build()
    }

    @Override
    protected void createSonarGerrit() {
        out.println("createSonarGerrit()")
        ChargingGerritSonarJobBuilder chargingGerritSonarJobBuilder = new ChargingGerritSonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory
                )

        chargingGerritSonarJobBuilder.build()
    }
}
