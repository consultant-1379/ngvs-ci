package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.util.GerritUtil

class Erms extends Project {
    public static String projectName = "erms"
    private String jiveMetaData = "https://arm.epk.ericsson.se/artifactory/proj-erms-release/com/ericsson/bss/rm/erms/jive/testcases/"
    private String versionLocation = "https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/ERMS/," +
            "https://arm.epk.ericsson.se/artifactory/proj-erms-release/com/ericsson/bss/rm/erms/opd/erms/;0.4.0"

    public Erms(){
        super.projectName = this.projectName
        super.releaseGoal = "-Dresume=false -Dgoals=deploy release:prepare release:perform"
        super.releaseDryrunGoal = "-Dresume=false -DdryRun=true -Dgoals=deploy release:prepare"
        extraMavenParameters = "-DskipGulp \\\n"
    }

    @Override
    public void init(parent) {
        super.init(parent)

        integrationTestRepository = gerritUser + "@" + gerritServer +
                                    ":29418/erms/com.ericsson.bss.rm.erms.integrationtest"
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-erms.xml"
    }

    @Override
    public void create(parent) {
        super.create(parent)
        super.createOvfBuildJob('ERMS/Build%20ERMS%20OVF', 'suites/build_ovf.xml',
                'build_ovf_\${TARGETHOST}.xml')

        super.createUmiTestJob( defaultTapasJobPath: 'ERMS/UMI%20ERMS%20Test',
                                suite: 'suites/umi_test.xml',
                                suiteFile: 'umi_test_\${TARGETHOST}.xml')

        super.createTargethostInstallJob(
                versionLocation: [versionLocation],
                defaultTapasJobPath: 'ERMS/Targethost%20ERMS%20Install',
                useDvFile: true,
                useCil: true,
                useJiveTests: true,
                jiveMetaData: jiveMetaData)
        super.createEpValidatorJob(
                versionLocation: versionLocation,
                defaultTapasJobPath: 'ERMS/ERMS%20EP%20Verification',
                useJiveTests: true,
                jiveMetaData: jiveMetaData,
                useDvFile: true
        )
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = []
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))

        String[] allRepositories = output.split('\n')
        for (repository in allRepositories) {
            if (repository.contains('com.ericsson.bss.rm.erms.')) {
                repositories.add(repository)
            }
        }

        repositories.add("eftf/erms")

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        if (repository == "eftf/erms") {
            repository = "eftf-erms"
        }
        String currentJobName = super.getJobName(repository)

        currentJobName = currentJobName.replace('com.ericsson.bss.rm.erms.', '')

        return currentJobName
    }

    @Override
    protected void createForRepository() {
        releaseGoal = DEFAULT_RELEASE_GOAL
        releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL

        if (jobName.equalsIgnoreCase('integrationtest')) {
            String releaseParameters = ' -DpreparationGoals="install" ' +
                    '-Darguments="-Dorg.ops4j.pax.url.mvn.settings=\${MAVEN_SETTINGS} -Dorg.ops4j.pax.url.mvn.localRepository=\${MAVEN_REPOSITORY} ' +
                    '--settings \${MAVEN_SETTINGS} -Dmaven.repo.local=\${MAVEN_REPOSITORY}"'

            releaseGoal += ' -Dgoals=deploy' + releaseParameters
            releaseDryrunGoal = DEFAULT_DRYRUN_RELEASE_GOAL + releaseParameters
        }

        super.createForRepository()
    }

    @Override
    protected void createIntegrationTestGerrit(int timeout) {
        super.createIntegrationTestGerrit(getIntegrationTestJobTimeout())
    }

    @Override
    protected int getDefaultJobTimeout() {
        if (jobName.equalsIgnoreCase('integrationtest')) {
            return getIntegrationTestJobTimeout()
        }
        else {
            return super.getDefaultJobTimeout()
        }
    }

    private int getIntegrationTestJobTimeout() {
        return super.getDefaultJobTimeout() * 2
    }
}
