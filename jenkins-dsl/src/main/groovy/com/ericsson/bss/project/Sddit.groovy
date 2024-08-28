package com.ericsson.bss.project

import com.ericsson.bss.Project

class Sddit extends Project {
    public String projectName = "sddit"

    public Sddit(){
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-charging.xml"
    }

    @Override
    protected List getRepositories() {
        List repositories = new ArrayList()

        repositories.add("charging/com.ericsson.bss.rm.sddit.jive")

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        String currentJobName = super.getJobName(repository)

        currentJobName = currentJobName.replace('com.ericsson.bss.rm.', '')

        return currentJobName
    }
}
