package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.util.GerritUtil

class Soivautotest extends Project {
    public static String projectName = "soivautotest"

    public Soivautotest(){
        super.projectName = this.projectName
        super.releaseGoal = "-Dresume=false -Dgoals=deploy release:prepare release:perform"
        super.releaseDryrunGoal = "-Dresume=false -DdryRun=true -Dgoals=deploy release:prepare"
    }

    @Override
    public void init(parent) {
        super.init(parent)

        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-charging.xml"
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = []
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains('com.ericsson.bss.rm.soivautotest.')) {
                repositories.add(repository)
            }
        }

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        String currentJobName = super.getJobName(repository)

        currentJobName = currentJobName.replace('com.ericsson.bss.rm.soivautotest.', '')

        return currentJobName
    }

    @Override
    protected void createForRepository() {
        out.println("projectName: " + projectName + ", jobname: " + jobName +
                ", gerritName: " + gerritName)

        createFolders()
        createSonar()
        createSonarGerrit()
        createUnittestGerrit()
        createDeploy()
    }
}
