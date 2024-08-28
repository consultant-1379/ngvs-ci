package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.DocToolsJobBuilder
import com.ericsson.bss.job.GerritSiteJobBuilder

class Eta extends Project {
    public static final String PROJECT_NAME = "eta"
    private final int timeout = 45

    public Eta() {
        super.projectName = PROJECT_NAME
        super.mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-eta.xml"
    }

    @Override
    public void create(Object parent) {
        super.create(parent)
        createToolsDocJob()
    }

    @Override
    protected List getRepositories() {
        List<String> etaRepositories = new ArrayList()
        etaRepositories.add("tools/eta/sonar-review-poster")
        return etaRepositories
    }

    @Override
    protected void createForRepository() {
        out.println("projectName: " + projectName + ", jobname: " + jobName + ", gerritName: " + gerritName)

        createFolders()
        createSite()
        createSonar()
        createDeploy()
        createSonarGerrit()
        createUnittestGerrit()
        createMvnDependencyTest()
        createSiteGerrit()
    }

    @Override
    protected String getJobName(String repositoryName) {
        return repositoryName.substring(repositoryName.lastIndexOf(delimiter) + 1)
    }

    protected void createSiteGerrit() {
        out.println("createSiteGerrit()")
        GerritSiteJobBuilder gerritSiteJobBuilder = new GerritSiteJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_site",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                timeoutForJob: timeout,
                projectName: projectName,
                dslFactory: dslFactory
                )

        gerritSiteJobBuilder.build()
    }

    private void createToolsDocJob(){
        out.println("createToolsDocJob()")
        DocToolsJobBuilder toolsDoc = new DocToolsJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                projectName: projectName,
                jobName: projectName + "_tools_document_generator",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                generateGUIconfig: true,
                dslFactory: dslFactory
            )
        toolsDoc.build()
    }
}
