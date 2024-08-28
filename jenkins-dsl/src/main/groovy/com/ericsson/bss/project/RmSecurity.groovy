package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.MvnGerritUnitTestJobBuilder

class RmSecurity extends Project {
    public String projectName = "rm-security"

    public RmSecurity() {
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)

        if (System.getProperty("user.name").equalsIgnoreCase("kascmadm")) {
            gerritUser = new String("chargingsystem_local")
        }
        gerritServer = GERRIT_FORGE_SERVER

        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-csi.xml"
        delimiter = '-'
    }

    @Override
    protected List getRepositories() {
        List repositories = new ArrayList()
        repositories.add("rm-security-init")
        repositories.add("rm-security-certificate-utils")
        repositories.add("rm-security-nsm")
        repositories.add("rm-security-shared")
        repositories.add("bss-security-proxy")

        out.println("repositories: " + repositories)
        return repositories
    }

    @Override
    protected void createUnittestGerrit() {
        if (jobName.equals('security-proxy')) {
            out.println("createUnittestGerrit()")
            MvnGerritUnitTestJobBuilder gerritUnitTestJobBuilder = new MvnGerritUnitTestJobBuilder(
                    gerritUser: gerritUser,
                    gerritServer: gerritServer,
                    jobName: folderName + "/" + jobName + "_gerrit_unit_test",
                    mavenRepositoryPath: mavenRepositoryPath,
                    mavenSettingsFile: mvnSettingFile,
                    gerritName: gerritName,
                    projectName: projectName,
                    verboseGerritFeedback: verboseGerritFeedback,
                    profilesToBeUsed: 'integrationtest',
                    dslFactory: dslFactory
                    )

            gerritUnitTestJobBuilder.build()
        }
        else {
            super.createUnittestGerrit()
        }
    }
}
