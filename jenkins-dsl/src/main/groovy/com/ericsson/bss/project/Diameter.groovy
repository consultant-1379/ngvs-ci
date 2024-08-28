package com.ericsson.bss.project

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.Project
import com.ericsson.bss.job.CreateBranchJobBuilder
import com.ericsson.bss.job.DeployJobBuilder
import com.ericsson.bss.job.GerritSonarJobBuilder
import com.ericsson.bss.job.SonarJobBuilder
import com.ericsson.bss.job.MvnGerritUnitTestJobBuilder
import com.ericsson.bss.job.diameter.DiameterCreateBranchJobBuilder
import com.ericsson.bss.job.diameter.DiameterDeliveryJobBuilder
import com.ericsson.bss.job.diameter.DiameterDeployJobBuilder
import com.ericsson.bss.job.diameter.DiameterMvnNativeJobBuilder
import com.ericsson.bss.job.diameter.DiameterPackageJobBuilder

class Diameter extends Project {
    public static String projectName = "diameter"
    private static final String SETTINGS_FILE = "kascmadm-settings_arm-diameter.xml"

    public Diameter() {
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)

        gerritServer = GERRIT_CENTRAL_SERVER

        mvnSettingFile = mvnSettingFilePath + SETTINGS_FILE
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = []

        repositories.add('diameter-base-java/diameter_base_java-source')

        return repositories
    }

    @Override
    protected void createForRepository() {
        createFolders()

        createDeploy()
        createReleaseDeploy()
        createReleaseBranch()
        createDiameterDelivery()
        createDiameterMvnNative()
        createDiameterPackage(getDefaultDeployJobTimeout())
        createSonar()
        createUnittestGerrit()
        createSonarGerrit()
    }

    @Override
    protected void createBuildMultiplePatchsetJob() {
        //Diameter has no dependencies between its repository so there is no need
        //to create the Jenkins job that can build multi patchset(s) from gerrit.
    }

    @Override
    protected void createDeploy(int timeout = getDefaultJobTimeout()) {
        out.println("createDeploy()")
        DeployJobBuilder deployJobBuilder = new DiameterDeployJobBuilder (
                workspacePath: workspacePathMesos,
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_deploy",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                dslFactory: dslFactory,
                blameMailList: blameMailList,
                prepareArm2Gask: prepareArm2Gask,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                timeoutForJob: timeout,
                timeoutForClone: timeout,
                )

        deployJobBuilder.build()
    }

    @Override
    protected void createReleaseDeploy(int timeout = getDefaultJobTimeout()) {
        out.println("createReleaseDeploy()")

        List releaseProjects = getReleaseBranches()

        if (releaseProjects.size() != 0 && releaseProjects != null) {
            releaseProjects.each {
                String branchName = it
                out.println('[INFO] Create release branch for: ' + branchName)
                DeployJobBuilder releaseBranchDeployJobBuilder = new DiameterDeployJobBuilder(
                        workspacePath: workspacePathMesos,
                        gerritUser: gerritUser,
                        gerritServer: gerritServer,
                        jobName: folderName + "/release/" + jobName + "_" + branchName.replace('/', '_'),
                        mavenRepositoryPath: mavenRepositoryPath,
                        mavenSettingsFile: mvnSettingFile,
                        gerritName: gerritName,
                        projectName: projectName,
                        dslFactory: dslFactory,
                        generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                        timeoutForJob: timeout,
                        timeoutForClone: timeout,
                        )

                releaseBranchDeployJobBuilder.buildReleaseBranch(branchName)
            }
        }
    }

    @Override
    protected void createReleaseBranch(int timeout = getDefaultJobTimeout()) {
        CreateBranchJobBuilder createBranchJobBuilder = new DiameterCreateBranchJobBuilder(
                workspacePath: workspacePathMesos,
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/release/_" + jobName + "_create_new_release_branch",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                dslFactory: dslFactory,
                timeoutForJob: timeout,
                timeoutForClone: timeout,
                )

        createBranchJobBuilder.build()
    }

    private void createDiameterDelivery(int timeout = getDefaultJobTimeout()) {
        out.println("createDeploy()")
        DiameterDeliveryJobBuilder diameterDeliveryJobBuilder = new DiameterDeliveryJobBuilder (
                workspacePath: workspacePathMesos,
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                releaseGoal: releaseGoal,
                releaseDryrunGoal: releaseDryrunGoal,
                jobName: folderName + "/" + jobName + "_delivery",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                dslFactory: dslFactory,
                blameMailList: blameMailList,
                prepareArm2Gask: prepareArm2Gask,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                timeoutForJob: timeout,
                timeoutForClone: timeout,
                extraMavenParameters: 'jar:jar javadoc:jar source:jar'
                )

        diameterDeliveryJobBuilder.build()
    }

    protected void createDiameterMvnNative(int timeout = getDefaultJobTimeout()) {
        DiameterMvnNativeJobBuilder diameterMvnNativeJobBuilder = new DiameterMvnNativeJobBuilder (
                workspacePath: workspacePathMesos,
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: AbstractJobBuilder.NO_VARNISH_MAVEN_SETTINGS_PATH + SETTINGS_FILE,
                jobName: folderName + "/" + jobName + "_native_deploy",
                gerritName: gerritName,
                projectName: projectName,
                timeoutForJob: timeout,
                timeoutForClone: timeout,
                dslFactory: dslFactory
                )

        diameterMvnNativeJobBuilder.build()
    }

    private void createDiameterPackage(int timeout = getDefaultJobTimeout()) {
        DiameterPackageJobBuilder diameterPackageJobBuilder = new DiameterPackageJobBuilder (
                workspacePath: workspacePathMesos,
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_package",
                gerritName: gerritName,
                projectName: projectName,
                timeoutForJob: timeout,
                timeoutForClone: timeout,
                dslFactory: dslFactory
                )

        diameterPackageJobBuilder.build()
    }

    @Override
    protected void createSonar(int timeout = getDefaultJobTimeout()) {
        out.println("createSonar()")
        SonarJobBuilder sonarJobBuilder = new SonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                dslFactory: dslFactory,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                generateCoberturaReport: true,
                timeoutForJob: timeout,
                timeoutForClone: timeout
                )

        sonarJobBuilder.build()
    }

    protected void createUnittestGerrit(int timeout = getDefaultJobTimeout()) {
        out.println("createUnittestGerrit()")
        MvnGerritUnitTestJobBuilder gerritUnitTestJobBuilder = new MvnGerritUnitTestJobBuilder (
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_unit_test",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName),
                timeoutForJob: timeout,
                timeoutForClone: timeout,
                runParallelThreads: false
                )

        gerritUnitTestJobBuilder.build()
    }

    protected void createSonarGerrit(int timeout = getDefaultJobTimeout()) {
        out.println("createSonarGerrit()")
        GerritSonarJobBuilder gerritSonarJobBuilder = new GerritSonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                generateCoberturaReport: true,
                timeoutForJob: timeout,
                timeoutForClone: timeout,
                dslFactory: dslFactory
                )

        gerritSonarJobBuilder.build()
    }

    @Override
    protected int getDefaultJobTimeout() {
        return super.getDefaultJobTimeout() * 4
    }
}
