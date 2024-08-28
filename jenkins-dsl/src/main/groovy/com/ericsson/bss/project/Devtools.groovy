package com.ericsson.bss.project

import com.ericsson.bss.Project

class Devtools extends Project {
    public String projectName = "devtools"

    public Devtools() {
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)

        if (System.getProperty("user.name").equalsIgnoreCase("kascmadm")) {
            gerritUser = new String("chargingsystem_local")
        }
        gerritServer = GERRIT_FORGE_SERVER

        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-charging.xml"
        delimiter = '_'
        super.createPrepareArm2GaskJob()
    }

    @Override
    protected List getRepositories() {
        List repositories = new ArrayList()
        repositories.add("devtools-docgen")
        repositories.add("devtools-avro")
        out.println("repositories: " + repositories)
        return repositories
    }

    @Override
    protected void createForRepository() {
        out.println("projectName: " + projectName + ", jobname: " + jobName +
                ", gerritName: " +gerritName)
        prepareArm2Gask = false
        createFolders()

        if (gerritName == "devtools-avro"){
            createSite()
            createSiteGerrit()
            prepareArm2Gask = true
        }
        createSonar()
        createSonarGerrit()
        createDeploy()
        createUnittestGerrit()
    }

    @Override
    protected void createFolders() {
        out.println("createFolders()")
        folderName = projectName + "/" + jobName
        dslFactory.folder(projectName) {}
        dslFactory.folder(folderName) {}
        jobName = jobName.replace('.', '_')
    }
}
