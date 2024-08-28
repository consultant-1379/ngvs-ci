package com.ericsson.bss.job

import com.ericsson.bss.AbstractJobBuilder

import javaposse.jobdsl.dsl.Job

public class MultiRepositoryReleaseJobBuilder extends AbstractJobBuilder {

    protected static final String JOB_DESCRIPTION = "<h2>This job release a bunch of repositories.</h2>"

    protected String buildName
    protected String releaseRepository
    protected String stagingRepository
    protected String mail
    protected List repositoryList
    protected boolean useReleaseVersion = true

    protected String mavenSettingsSecretFile = '/home/kascmadm/.m2/settings-security.xml'

    public Job build() {
        timeoutForJob = 180
        initProject(dslFactory.freeStyleJob(projectName + "_multi_repository_release"))

        addReleaseConfig()

        return job
    }

    private void addReleaseConfig() {
        job.with {
            description(DSL_DESCRIPTION + JOB_DESCRIPTION)

            parameters {
                choiceParam('BUILD_TYPE', [
                    'CompileAndTest',
                    'BuildAndPublish',
                ], 'Select a build mode:<br />\n' +
                'CompileAndTest - Do everything except deploy and publish<br/>\n' +
                'BuildAndPublish - Perform a complete build, including publish<br/>')

                booleanParam('SKIP_TESTS', false, 'Skip running the tests.')

                booleanParam('USE_VERSION_RANGES', false, 'Use version ranges for snapshot dependencies.')
            }

            triggers {
                cron('H */12 * * *')

                if (!useReleaseVersion) {
                    upstream('charging.core/release/release_deploy', 'SUCCESS')
                }
            }

            Map envVarMap = getInjectVariables()
            envVarMap.put('RELEASE_SCRIPT_REPOSITORIES', '\${WORKSPACE}/repositories.txt')
            envVarMap.put('SETTINGS_SECURITY', mavenSettingsSecretFile)
            envVarMap.put('PATH', '\${JAVA_HOME}/bin:' + envVarMap.get('PATH'))
            injectEnv(envVarMap)

            addTimeoutConfig()

            steps {
                if (symlinkWorkspace) {
                    shell(symlinkMesosWorkSpace())
                }
                shell(createHomeTmpFolder())
                shell(getReleaseRepositoriesToFileCommand())
                shell(getMultiReleaseScriptArtifactCommand())
                shell(getRunMultiScriptCommand())
            }

            publishers {
                if (mail != null && !mail.equalsIgnoreCase("")) {
                    mailer(mail, false, false)
                }

                wsCleanup()
            }
        }
    }

    protected String getReleaseRepositoriesToFileCommand() {
        String reposiotryToFile = getShellCommentDescription("Repositories to be released")

        for (String repository:repositoryList) {
            reposiotryToFile +="echo \"" + repository + "\" >> \${RELEASE_SCRIPT_REPOSITORIES}" + "\n"
        }

        return reposiotryToFile
    }

    protected String getMultiReleaseScriptArtifactCommand() {
        String version = 'LATEST'

        if (useReleaseVersion) {
            version = 'RELEASE'
        }

        String newLine = ' \\\n'

        return getShellCommentDescription("Download multi release script") +
                'mvn' + newLine +
                '-DgroupId=com.ericsson.bss.rm.charging.release' + newLine +
                '-DartifactId=script' + newLine +
                '-Dversion=' + version + newLine +
                '-Dtransitive=false' + newLine +
                '-DremoteRepositories=https://arm.epk.ericsson.se/artifactory/proj-charging-dev/' + newLine +
                '-Ddest=multi_release_script.jar' + newLine +
                'org.apache.maven.plugins:maven-dependency-plugin:2.4:get' + newLine +
                getMavenGeneralBuildParameters()
    }

    protected String getRunMultiScriptCommand() {
        final String DELIMITER = ' '
        StringBuilder runCommand = new StringBuilder()

        runCommand.append(getShellCommentDescription("Trigger multi release build"))
        runCommand.append('java')
        runCommand.append(DELIMITER)
        runCommand.append('-Dscript.buildName="' + buildName + '"')
        runCommand.append(DELIMITER)
        runCommand.append('-Dscript.buildNumber="\${BUILD_NUMBER}"')
        runCommand.append(DELIMITER)
        runCommand.append('-Dscript.releaseRepository="' + releaseRepository + '"')
        runCommand.append(DELIMITER)
        runCommand.append('-Dscript.stagingRepository="' + stagingRepository + '"')
        runCommand.append(DELIMITER)
        runCommand.append('-Dscript.gitWorkArea="\${WORKSPACE}/.gitworkarea"')
        runCommand.append(DELIMITER)
        runCommand.append('-Dscript.repositoryList="file://\${RELEASE_SCRIPT_REPOSITORIES}"')
        runCommand.append(DELIMITER)
        runCommand.append('-Dscript.localMavenRepository="\${WORKSPACE}/.scriptLocalMavenRepository"')
        runCommand.append(DELIMITER)
        runCommand.append('-Dscript.M2_HOME="\${M2_HOME}"')
        runCommand.append(DELIMITER)
        runCommand.append('-Dscript.mavenSettings="\${MAVEN_SETTINGS}"')
        runCommand.append(DELIMITER)
        runCommand.append('-Dscript.settingsSecurity="\${SETTINGS_SECURITY}"')
        runCommand.append(DELIMITER)
        runCommand.append('-Dscript.buildType="\${BUILD_TYPE}"')
        runCommand.append(DELIMITER)
        runCommand.append('-Dscript.skipTests="\${SKIP_TESTS}"')
        runCommand.append(DELIMITER)
        runCommand.append('-Dscript.useVersionRanges="\${USE_VERSION_RANGES}"')
        runCommand.append(DELIMITER)
        runCommand.append('-Djava.io.tmpdir="\${WS_TMP}"')
        runCommand.append(DELIMITER)
        runCommand.append('-jar')
        runCommand.append(DELIMITER)
        runCommand.append('multi_release_script.jar')

        return runCommand.toString()
    }
}
