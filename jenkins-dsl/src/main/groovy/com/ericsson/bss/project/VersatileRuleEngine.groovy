package com.ericsson.bss.project

import com.ericsson.bss.Project

class VersatileRuleEngine extends Project {
    public String projectName = "bssf_versatile_rule_engine"

    public VersatileRuleEngine() {
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
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = new ArrayList()

        String output = getGerritforgeProjects()

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains(projectName)) {
                repositories.add(repository)
            }
        }

        return repositories
    }
}
