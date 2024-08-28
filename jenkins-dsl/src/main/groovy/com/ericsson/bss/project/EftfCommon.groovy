package com.ericsson.bss.project

import com.ericsson.bss.Project

class EftfCommon extends Project {

    public static String projectName = "eftf.common"

    public EftfCommon(){
        super.projectName = projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-charging.xml"
    }

    @Override
    protected List getRepositories() {
        List repositories = new ArrayList()

        repositories.add("eftf/rm-common")
        out.println("repositories: " + repositories)

        return repositories
    }
}
