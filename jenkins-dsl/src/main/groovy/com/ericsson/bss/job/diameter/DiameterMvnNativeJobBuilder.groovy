//**********************************************************************
// Copyright (c) 2016 Telefonaktiebolaget LM Ericsson, Sweden.
// All rights reserved.
// The Copyright to the computer program(s) herein is the property of
// Telefonaktiebolaget LM Ericsson, Sweden.
// The program(s) may be used and/or copied with the written permission
// from Telefonaktiebolaget LM Ericsson or in accordance with the terms
// and conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.
// **********************************************************************
package com.ericsson.bss.job.diameter

import javaposse.jobdsl.dsl.Job

import com.ericsson.bss.AbstractGerritJobBuilder

class DiameterMvnNativeJobBuilder extends AbstractGerritJobBuilder {

    public Job build() {
        runXvfb = false //No support in SunOS and Sparc
        injectPortAllocation = ""

        initProject(dslFactory.matrixJob(jobName))
        addNativeJobConfig()

        return job
    }

    protected String getScriptToSetDynamicTimeout() {
        return "" //Groovy will not work on SunOS and Sparc
    }

    private void addNativeJobConfig() {
        job.with {

            description('<h2>Will compile and deploy a package of the native libraries</h2>\n' +
                    '<p>Jobs will be spread to the specific slaves, to be able to compile the native libraries.</p>' +
                    '<p>The job will trigger the ant task to build the libraries. It will then use maven for creating a package and deploy this to ARM.</p>')

            parameters {
                stringParam('BRANCH', 'master', 'Git branch to be built.')
            }

            environmentVariables { env('JOB_TIMEOUT', timeoutForJob) }

            addGitRepository(gerritName, '\${BRANCH}')

            axes {
                label('label', 'Linux_redhat_6.2_x86_64_mesos', 'SunOS_5.10_i386', 'SunOS_5.10_sparc')
            }

            addTimeoutConfig()

            Map variables = [:]
            variables.put('ANT_HOME', '/opt/local/dev_tools/ant/1.8.2')
            variables.putAll(getInjectVariables())
            variables.put('PATH', '${ANT_HOME}/bin/:' + injectVariables['PATH'])
            injectEnv(variables)

            steps {
                shell(gitInitiateRepository("\${WORKSPACE}"))
                shell(gitConfig("\${WORKSPACE}"))
                shell(getGitCache("\${WORKSPACE}"))
                shell(archDependingJavaHome() + '\n\n' +
                        buildAllModules() + '\n\n' +
                        getHelpEvaluateWorkaround() + '\n\n' +
                        gitCheckoutCommand() + '\n\n' +
                        getShellCommentDescription("Create tmp folder for maven") + createHomeTmpFolder() + '\n\n' +
                        nativeProject() + '\n\n' +
                        getHelpEvaluateWorkaround() + '\n\n' +
                        getProjectInformation() + '\n\n' +
                        mavenBuildCommand() + '\n\n' +
                        deployFile() + '\n\n' +
                        andBuildNative())
            }
        }
    }

    private String getHelpEvaluateWorkaround() {
        return getShellCommentDescription("Workaround to download all needed dependnecies for help:evaluate") +
                'mvn help:evaluate --settings \${MAVEN_SETTINGS} -Dmaven.repo.local=\${MAVEN_REPOSITORY}'
    }

    @Override
    protected String getMavenVersion(String branchName) {
        return "3.0.5" //Otherwise result in Unsupported major.minor version 51.0
    }

    private String archDependingJavaHome() {
        return getShellCommentDescription("Set JAVA_HOME depending on arch") +
                'if [[ `uname -s` = "Linux" ]]; then\n' +
                '  export JAVA_HOME=/opt/local/dev_tools/java/x64/latest-1.6\n' +
                'fi\n' +
                'if [[ `uname -s` = "SunOS" ]]; then\n' +
                '  export JAVA_HOME=/opt/local/dev_tools/java/latest-1.6\n' +
                'fi'
    }

    private String buildAllModules() {
        return getShellCommentDescription("Build all modules") +
                'mvn clean install \\\n' +
                "-DskipTests \\\n" +
                "--fail-never \\\n" +
                getMavenGeneralBuildParameters()
    }

    private String gitCheckoutCommand() {
        return getShellCommentDescription("Checkout release tag or development branch") +
                'if ([ "\${MVN_RELEASE_VERSION}" ] && [ "\${MVN_ISDRYRUN}" == false ]); then\n' +
                '  GIT_TAG_ARTIFACT_ID=' + evaluateMavenVariableValue('project.artifactId') + '\n' +
                '  git checkout \${GIT_TAG_ARTIFACT_ID}-\${MVN_RELEASE_VERSION}\n' +
                'else\n' +
                '  git checkout ${BRANCH}\n' +
                'fi'
    }

    private String evaluateMavenVariableValue(String variable) {
        return '\$(mvn help:evaluate -Dexpression=' + variable + ' --settings \${MAVEN_SETTINGS} -Dmaven.repo.local=\${MAVEN_REPOSITORY} ' +
                '| egrep -v "(^\\[|Download\\w+:)")'
    }

    private String nativeProject() {
        return getShellCommentDescription("The native project to build") +
                'cd sctp-native/'
    }

    private String getProjectInformation() {
        return getShellCommentDescription("Get project info") +
                'VERSION=' + evaluateMavenVariableValue('project.version') + '\n' +
                'ARTIFACT_ID=' + evaluateMavenVariableValue('project.artifactId') + '\n' +
                'GROUP_ID=' + evaluateMavenVariableValue('project.groupId') + '\n' +
                'CLASSIFIER=' + evaluateMavenVariableValue('os.classpath') + '\n' +
                'URL=' + evaluateMavenVariableValue('project.distributionManagement.repository.url') + '\n' +
                'if [[ \$VERSION =~ .*-SNAPSHOT.* ]]; then\n' +
                '  URL=' + evaluateMavenVariableValue('project.distributionManagement.snapshotRepository.url') + '\n' +
                'fi'
    }

    protected String mavenBuildCommand() {
        String cmd = getShellCommentDescription("Maven build command") +
                "mvn \\\n" +
                "clean antrun:run assembly:single install \\\n" +
                "--fail-never \\\n"

        if (mavenProjectLocation){
            cmd += " -f " + mavenProjectLocation + " \\\n"
        }

        if (profilesToBeUsed != null && profilesToBeUsed != "") {
            cmd += '-P' + profilesToBeUsed + ' \\\n'
        }

        cmd += "-Dsurefire.useFile=false \\\n" +
                getMavenGeneralBuildParameters()

        return cmd
    }

    private String deployFile() {
        return getShellCommentDescription("Deploy the native package") +
                'mvn deploy:deploy-file -Dfile=target/\${ARTIFACT_ID}-\${VERSION}-\${CLASSIFIER}.tar.gz \\\n' +
                "--fail-never \\\n" +
                '-DgroupId=\${GROUP_ID} -DartifactId=\${ARTIFACT_ID} -Dversion=\${VERSION} -Dclassifier=\${CLASSIFIER} \\\n' +
                '-Dpackaging=tar.gz -DgeneratePom=false -Durl=\${URL} -DrepositoryId=arm \\\n' +
                getMavenGeneralBuildParameters()
    }

    private String andBuildNative() {
        return getShellCommentDescription("Need to build with Ant for legacy branches") +
                "cd \${WORKSPACE}\n" +
                "/opt/local/dev_tools/ant/1.8.2/bin/ant compile-native"
    }

    @Override
    protected void archiveArtifacts() {
        job.with{
            publishers {
                archiveArtifacts {
                    allowEmpty(true)
                    pattern(getJavaCrashLogToArchive() + ',' + getDiameterArtifactsToArchive() + ',**/target/*tar.gz')
                }
            }
        }
    }

    private String getJavaCrashLogToArchive() {
        return '**/hs_err_pid*.log'
    }

    private String getDiameterArtifactsToArchive() {
        return '*/target/liberisctp*.so'
    }
}
