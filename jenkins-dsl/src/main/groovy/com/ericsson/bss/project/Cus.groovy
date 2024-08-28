package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.cus.ui.CusUiNpmDeployReleaseFromMasterJobBuilder
import com.ericsson.bss.job.cus.ui.CusUiNpmDeployReleaseFromPatchBranchJobBuilder
import com.ericsson.bss.job.cus.ui.CusUiNpmDeploySnapshotJobBuilder
import com.ericsson.bss.job.cus.ui.assets.CusUiAssetsNpmDeployReleaseFromMasterJobBuilder
import com.ericsson.bss.job.cus.ui.assets.CusUiAssetsNpmDeployReleaseFromPatchBranchJobBuilder
import com.ericsson.bss.job.cus.ui.assets.CusUiAssetsNpmDeploySnapshotJobBuilder
import com.ericsson.bss.util.GerritUtil

class Cus extends Project {

    public static final HashMap<String, List> VALUESOFRESOURCEPROFILES = ['TestSystem':['4', '8192'], 'Default':['', '']]

    public Cus() {
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-cus.xml"
        targetMachine = "sekarm125"
        projectName = "cus"
        npmRegistry = "https://arm.epk.ericsson.se/artifactory/api/npm/proj-rm-ui-npm"
        addNpmRepositories()
    }

    public void addNpmRepositories() {
        npmRepositories.add("cus/com.ericsson.bss.rm.ui.assets")
        npmRepositories.add("cus/com.ericsson.bss.rm.cus.ui")
    }

    @Override
    public boolean runProject(String projectName) {
        return projectName == this.projectName
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = []
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains('cus/')) {
                repositories.add(repository)
            }
        }

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
        super.createTargethostInstallJob(
            versionLocation: ['https://arm.epk.ericsson.se/artifactory/proj-cus-release-local/com/ericsson/bss/rm/cus/cuspackage/'],
            defaultTapasJobPath: 'CUS/Targethost%20CUS%20Install',
            valuesOfResourceProfiles: VALUESOFRESOURCEPROFILES,
            useDvFile: true,
            useJiveTests: true,
            jiveMetaData: 'https://arm.epk.ericsson.se/artifactory/proj-cus-release/com/ericsson/bss/rm/cus/jivetests/')
        super.createCreateClusterJob()
        super.createRemoveClusterJob()
        super.createAddRemoveNightlyFullTargethostInstall(
                onlyTargethostInstallation: true
        )
        super.createNightlyFullTargethostInstall(
                cronTrigger: 0,
                targethostInstallParameters:
                        [
                                "TARGETHOST": "targethost"
                        ]
        )
    }

    @Override
    protected void createSite() {
        int increaseTimeout = 2
        super.createSite(getDefaultJobTimeout() * increaseTimeout)
    }

    @Override
    protected void createDeploy() {
        super.createDeploy(getDefaultDeployJobTimeout() * 2)
    }

    @ Override
    protected void createNpmDeploySnapshotJob(params) {
        switch (gerritName) {
            case "cus/com.ericsson.bss.rm.ui.assets":
                params.NpmDeploySnapshotJobBuilderClass = CusUiAssetsNpmDeploySnapshotJobBuilder
                break

            case "cus/com.ericsson.bss.rm.cus.ui":
                params.NpmDeploySnapshotJobBuilderClass = CusUiNpmDeploySnapshotJobBuilder
                break
        }

        super.createNpmDeploySnapshotJob(params)
    }

    @ Override
    protected void createNpmDeployReleaseFromMasterJob(params) {
        switch (gerritName) {
            case "cus/com.ericsson.bss.rm.ui.assets":
                params.NpmDeployReleaseFromMasterJobBuilderClass = CusUiAssetsNpmDeployReleaseFromMasterJobBuilder
                break

            case "cus/com.ericsson.bss.rm.cus.ui":
                params.NpmDeployReleaseFromMasterJobBuilderClass = CusUiNpmDeployReleaseFromMasterJobBuilder
                break
        }

        super.createNpmDeployReleaseFromMasterJob(params)
    }

    @ Override
    protected void createNpmDeployReleaseFromPatchBranchJob(params) {
        switch (gerritName) {
            case "cus/com.ericsson.bss.rm.ui.assets":
                params.NpmDeployReleaseFromPatchBranchJobBuilderClass = CusUiAssetsNpmDeployReleaseFromPatchBranchJobBuilder
                break

            case "cus/com.ericsson.bss.rm.cus.ui":
                params.NpmDeployReleaseFromPatchBranchJobBuilderClass = CusUiNpmDeployReleaseFromPatchBranchJobBuilder
                break
        }

        super.createNpmDeployReleaseFromPatchBranchJob(params)
    }
}
