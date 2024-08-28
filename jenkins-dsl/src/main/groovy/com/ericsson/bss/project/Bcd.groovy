package com.ericsson.bss.project

import com.ericsson.bss.job.DeployJobBuilder
import com.ericsson.bss.job.GerritDependencyTestJobBuilder
import com.ericsson.bss.job.GerritSonarJobBuilder
import com.ericsson.bss.job.MvnGerritUnitTestJobBuilder
import com.ericsson.bss.job.SonarJobBuilder
import com.ericsson.bss.job.bcd.BcdDeployJobBuilder
import com.ericsson.bss.job.bcd.BcdGerritDependencyTestJobBuilder
import com.ericsson.bss.job.bcd.BcdGerritSonarJobBuilder
import com.ericsson.bss.job.bcd.BcdGerritUnitTestJobBuilder
import com.ericsson.bss.job.bcd.BcdSonarJobBuilder
import com.ericsson.bss.Project

class Bcd extends Project {
    public String projectName = "bcd"
    public static final String JDK_VERSION = 'BCD Java'

    public Bcd() {
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)

        gerritServer = GERRIT_EPK_SERVER
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-bcd.xml"
        generateSiteJobs = false
    }

    @Override
    protected List getRepositories() {
        List repositories = []

        repositories.add("bcd/top")
        repositories.add("bcd/agent")
        repositories.add("bcd/common")
        repositories.add("bcd/devbuild")
        repositories.add("bcd/integration")
        repositories.add("bcd/server")

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected void createDeploy() {
        out.println("createDeploy()")

        String mavenProjectLocation = ""
        String jobName = folderName + "/" + jobName + "_deploy"

        if (gerritName == 'bcd/devbuild') {
            mavenProjectLocation = "bcd.client.sim"
            jobName = folderName + "/test_clientsimulator_deploy"
        }

        DeployJobBuilder deployJobBuilder = new BcdDeployJobBuilder (
                mavenProjectLocation: mavenProjectLocation,
                workspacePath: workspacePathMesos,
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                releaseGoal: releaseGoal,
                releaseDryrunGoal: releaseDryrunGoal,
                jobName: jobName,
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                dslFactory: dslFactory,
                blameMailList: blameMailList,
                prepareArm2Gask: prepareArm2Gask,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                extraMavenParameters: "-Denvironment=target"
                )

        deployJobBuilder.build()
    }

    @Override
    protected void createMvnDependencyTest() {
        out.println("createMvnDependencyTest()")
        GerritDependencyTestJobBuilder gerritDependencyTestJobBuilder = new BcdGerritDependencyTestJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_dependency_test",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory
                )

        gerritDependencyTestJobBuilder.build()
    }

    @Override
    protected void createSonarGerrit() {
        out.println("createSonarGerrit()")
        GerritSonarJobBuilder gerritSonarJobBuilder = new BcdGerritSonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName)
                )

        gerritSonarJobBuilder.build()
    }

    @Override
    protected void createUnittestGerrit() {
        out.println("createUnittestGerrit()")
        MvnGerritUnitTestJobBuilder gerritUnitTestJobBuilder = new BcdGerritUnitTestJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_unit_test",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName)
                )

        gerritUnitTestJobBuilder.build()
    }

    @Override
    protected void createSonar() {
        out.println("createSonar()")
        SonarJobBuilder sonarJobBuilder = new BcdSonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                dslFactory: dslFactory,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName)
                )

        sonarJobBuilder.build()
    }

    @Override
    protected void createReleaseDeploy() {
        out.println("createReleaseDeploy()")

        List releaseProjects = getReleaseBranches()

        if (releaseProjects.size() != 0 && releaseProjects != null) {
            releaseProjects.each {
                String branchName = it
                out.println('[INFO] Create release branch for: ' + branchName)
                DeployJobBuilder releaseBranchDeployJobBuilder = new BcdDeployJobBuilder(
                        workspacePath: workspacePathMesos,
                        gerritUser: gerritUser,
                        gerritServer: gerritServer,
                        releaseGoal: releaseGoal,
                        releaseDryrunGoal: releaseDryrunGoal,
                        jobName: folderName + "/release/" + jobName + "_" + branchName.replace('/', '_'),
                        mavenRepositoryPath: mavenRepositoryPath,
                        mavenSettingsFile: mvnSettingFile,
                        gerritName: gerritName,
                        projectName: projectName,
                        dslFactory: dslFactory,
                        generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName)
                        )

                releaseBranchDeployJobBuilder.buildReleaseBranch(branchName)
            }
        }
    }

    @Override
    protected void createReleaseSonar() {
        out.println("createReleaseSonar()")

        List releaseProjects = getReleaseBranches()

        if (releaseProjects.size() != 0 && releaseProjects != null) {
            releaseProjects.each {
                String branchName = it
                out.println('[INFO] Create sonar job for: ' + branchName)
                SonarJobBuilder sonarJobBuilder = new BcdSonarJobBuilder(
                        gerritUser: gerritUser,
                        gerritServer: gerritServer,
                        jobName: folderName + "/release/" + jobName + "_" + branchName.replace('/', '_') + '_sonar',
                        mavenRepositoryPath: mavenRepositoryPath,
                        mavenSettingsFile: mvnSettingFile,
                        gerritName: gerritName,
                        dslFactory: dslFactory,
                        branchName: branchName,
                        generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName)
                        )

                sonarJobBuilder.build()
            }
        }
    }

    @Override
    protected void createReleaseSonarGerrit() {
        out.println("createReleaseSonarGerrit()")

        List releaseProjects = getReleaseBranches()

        if (releaseProjects.size() != 0 && releaseProjects != null) {
            releaseProjects.each {
                String branchName = it
                out.println('[INFO] Create sonar gerrit job for: ' + branchName)

                GerritSonarJobBuilder gerritSonarJobBuilder = new BcdGerritSonarJobBuilder(
                        gerritUser: gerritUser,
                        gerritServer: gerritServer,
                        jobName: folderName + "/release/" + jobName + "_" + branchName.replace('/', '_') + '_gerrit_sonar',
                        mavenRepositoryPath: mavenRepositoryPath,
                        mavenSettingsFile: mvnSettingFile,
                        gerritName: gerritName,
                        projectName: projectName,
                        verboseGerritFeedback: verboseGerritFeedback,
                        dslFactory: dslFactory,
                        branchName: branchName,
                        generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName)
                        )

                gerritSonarJobBuilder.build()
            }
        }
    }
}
