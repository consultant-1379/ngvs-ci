//**********************************************************************
// Copyright (c) 2016 Telefonaktiebolaget LM Ericsson, Sweden.
// All rights reserved.
// The Copyright to the computer program(s) herein is the property of
// Telefonaktiebolaget LM Ericsson, Sweden.
// The program(s) may be used and/or copied with the written permission
// from Telefonaktiebolaget LM Ericsson or in accordance with the terms
// and conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.
// **********************************************************************
package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.GerritSonarJobBuilder
import com.ericsson.bss.job.SonarJobBuilder
import com.ericsson.bss.job.aircakecommon.AirCakeCommonSonarJobBuilder

class AirCakeCommon extends Project {
    public String projectName = "AirCakeCommon"
    private String[] featureBranches = [
            'develop',
            'vs5_buc_security',
            'vs5_buc_srsso'
    ]

    public AirCakeCommon(){
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-air.xml"
    }

    @Override
    protected List getRepositories() {
        List repositories = new ArrayList()

        repositories.add("commonosgi/rm.common")

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected void createForRepository() {
        out.println("projectName: " + projectName + ", jobname: " + jobName + ", gerritName: " +gerritName)

        createFolders()
        createSonar()
        createSonarGerrit()
    }

    @Override
    protected void createFolders() {
        out.println("createFolders()")
        //Removing the release folder, not used in this development process
        folderName = projectName + "/" + jobName
        dslFactory.folder(projectName) {}
        dslFactory.folder(folderName) {}
        jobName = jobName.replace('.', '_')
    }

    @Override
    protected void createSonar() {
        out.println("createSonar()")
        SonarJobBuilder sonarJobBuilder = new AirCakeCommonSonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                dslFactory: dslFactory,
                sonarProfile: "NGVS Design Rules",
                mailRecipients : 'cc:PDLECBMDCG@pdl.internal.ericsson.com, cc:abhinav.pratap.srivastava@ericsson.com',
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName)
                )

        sonarJobBuilder.build()

        featureBranches.each {
            SonarJobBuilder sonarFeatureBranchesJobBuilder = new AirCakeCommonSonarJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_" + it + "_sonar",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    dslFactory: dslFactory,
                    branchName: it,
                    sonarProfile: "NGVS Design Rules",
                    mailRecipients : 'cc:PDLECBMDCG@pdl.internal.ericsson.com, cc:abhinav.pratap.srivastava@ericsson.com',
                    generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName)
            )

            sonarFeatureBranchesJobBuilder.build()
        }
    }

    @Override
    protected void createSonarGerrit() {
        out.println("createSonarGerrit()")
        GerritSonarJobBuilder gerritSonarJobBuilder = new GerritSonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory,
                sonarProfile: "NGVS Design Rules",
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName)
                )

        gerritSonarJobBuilder.build()

        featureBranches.each {
            GerritSonarJobBuilder gerritSonarFeatureBranchesJobBuilder = new GerritSonarJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_" + it + "_gerrit_sonar",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    verboseGerritFeedback: verboseGerritFeedback,
                    dslFactory: dslFactory,
                    projectName: projectName,
                    branchName: it,
                    sonarProfile: "NGVS Design Rules",
                    generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName)
            )
            gerritSonarFeatureBranchesJobBuilder.build()
        }
    }
}
