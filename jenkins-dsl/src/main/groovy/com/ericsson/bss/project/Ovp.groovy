package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.util.GerritUtil

class Ovp extends Project {
    public static String projectName = "ovp"

    public Ovp(){
        super.projectName = this.projectName
        super.releaseGoal = "-Dresume=false -Dgoals=deploy release:prepare release:perform"
        super.releaseDryrunGoal = "-Dresume=false -DdryRun=true -Dgoals=deploy release:prepare"
    }

    @Override
    public void init(parent) {
        super.init(parent)

        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-ovp.xml"
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = []
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains('com.ericsson.bss.rm.ovp.')) {
                repositories.add(repository)
            }
        }

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        String currentJobName = super.getJobName(repository)

        currentJobName = currentJobName.replace('com.ericsson.bss.rm.ovp.', '')

        return currentJobName
    }
}
