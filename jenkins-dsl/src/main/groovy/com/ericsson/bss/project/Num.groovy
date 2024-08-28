package com.ericsson.bss.project

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.job.num.NumTargethostInstallJobBuilder
import com.ericsson.bss.Project
import com.ericsson.bss.util.GerritUtil

class Num extends Project {
    public static String projectName = "num"
    private String releaseParametersForIntegrationTest = ' -DpreparationGoals="install" -Darguments="' + AbstractJobBuilder.MVN_SETTINGS + ' ' +
            AbstractJobBuilder.MVN_REPOSIOTRY + '"'

    public Num(){
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)

        integrationTestRepository = gerritUser + "@" + gerritServer +
                                    ":29418/num/com.ericsson.bss.rm.num.integrationtest"
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-num.xml"
    }

    @Override
    public void create(Object parent) {
        super.create(parent)
        super.createOvfBuildJob('NUM/Build%20NUM%20OVF', 'suites/build_ovf.xml',
                                'build_ovf_\${TARGETHOST}.xml')
        super.createUmiTestJob( defaultTapasJobPath: 'NUM/UMI%20NUM%20Test',
                                suite: 'suites/umi_test.xml',
                                suiteFile: 'umi_test_\${TARGETHOST}.xml')
        super.createTargethostInstallJob(
            installNodeName: 'vmx-num001',
            targethostInstallJobBuilderClass: NumTargethostInstallJobBuilder,
            valuesOfResourceProfiles: ['TestSystem':['2', '4096'], 'Default':['8', '8192']],
            versionLocation: ['https://arm.epk.ericsson.se/artifactory/simple/proj-num-release-local/com/ericsson/bss/rm/num/integration/numpackage'],
            defaultTapasJobPath: 'NUM/NUM%20Targethost%20Install',
            useDvFile: true,
            useTestData: true,
            useAppGroup: true)
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = []
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains('com.ericsson.bss.rm.num.')) {
                repositories.add(repository)
            }
        }
        repositories.add("eftf/num")

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected void createDeploy() {

        if (jobName.contains('integrationtest')) {
            releaseGoal = DEFAULT_RELEASE_GOAL + ' ' + releaseParametersForIntegrationTest
            releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL + ' ' + releaseParametersForIntegrationTest
        }

        super.createDeploy()
    }

    @Override
    protected String getJobName(String repository) {
        if (repository == "eftf/num") {
            repository = "eftf-num"
        }

        String currentJobName = super.getJobName(repository)

        currentJobName = currentJobName.replace('com.ericsson.bss.rm.num.', '')

        return currentJobName
    }
}
