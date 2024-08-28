package com.ericsson.bss.job

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.util.GitUtil
import javaposse.jobdsl.dsl.Job

public class SonarJobBuilder extends AbstractJobBuilder {

    public static final String JOB_DESCRIPTION = "<h2>Job that analyze the code and publish the result in SonarQube.</h2>\n" +
    "<p>The job automatically starts when new commits are submitted.</p>\n" +
    "<p>SonarQube publish the data from different code analyze plugins for example findbugs, pmd, checkstyle and many more.</p>\n" +
    "Related questions\n" +
    "<ul>\n" +
    "  <li><a href=\"https://eqna.lmera.ericsson.se/questions/32718\">Who is responsible for coding convention?</a></li>\n" +
    "<ul>\n" +
    BSSF_MAVEN_CI_DESCRIPTION

    protected List<String> shells
    protected boolean generateCoberturaReport
    protected String sonarProfile = "BMARP Design Rules"
    protected String branchName = "master"

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))

        shells = []
        initShellJobs()

        addSonarConfig()
        return job
    }

    protected void initShellJobs(){
        shells.add(cleanUpWorkspaceMesos("\${WORKSPACE}"))
        shells.add(removeOldArtifacts())
        shells.add(gitConfig("\${WORKSPACE}"))
        if (generateGUIconfig) {
            shells.add(gconfWorkspaceWorkaround())
        }
        if (branchName != "master" && branchName.contains('/')) {
            shells.add(getBranchRenameCommand())
        }
        shells.add(getCoverageCommand())
        if (generateGUIconfig) {
            shells.add(getCDTSonarTesReportWorkaround())
        }
    }

    protected void addSonarConfig() {
        job.with {
            description(JOB_DESCRIPTION)

            addGitRepository(gerritName, branchName)
            customWorkspace(CUSTOM_WORKSPACE_MESOS)
            addTimeoutConfig()
            triggers {
                if (GitUtil.isLocatedInGitolite(gerritName)) {
                    scm(SCM_POLLING_FREQUENT)
                } else {
                    scm(SCM_POLLING + '\n# Realtime pushed by the eta_gitscmpoll_trigger job')
                }
            }
            steps {
                for (shellStep in shells) {
                    shell(shellStep)
                }
            }
            injectEnv(getInjectVariables())
            addSonarNature()
            addExtendableEmail()

            publishers { wsCleanup() }
        }
    }

    protected String getCoverageCommand() {
        String mavenSubProjectCmd = ""
        if (mavenProjectLocation) {
            mavenSubProjectCmd = " -f " + mavenProjectLocation
        }
        String cmd = getShellCommentDescription("Command to generate coverage report") +
                "mvn " + mavenSubProjectCmd +  " \\\n"

        if (generateCoberturaReport) {
            cmd += "clean cobertura:cobertura -Dcobertura.report.format=xml install \\\n"
        }
        else {
            cmd += "clean " + JACOCO_AGENT + " install \\\n" +
                    "-Dcobertura.skip=true \\\n"
        }

        if (profilesToBeUsed != null && profilesToBeUsed != "") {
            cmd += ' -P' + profilesToBeUsed + ' '
        }

        cmd += "-Dsurefire.useFile=false \\\n" +
                getMavenGeneralBuildParameters() + " \\\n" +
                "-Dmaven.test.failure.ignore=true"

        return cmd
    }

    protected String getBranchRenameCommand() {
        String cmd = getShellCommentDescription("Rename branch for sonar analysis") +
                "git branch -f " + branchName.replace('/', '_')
        return cmd
    }

    protected void addSonarNature() {
        //this is required since a method cannot be executed directly in mavenInstallation() step
        String mavenInstallation = getMavenInstallationName()
        String sonarAdditionalProperties = getSonarAdditionalProperties()

        job.with {
            configure { project ->
                project / publishers << 'hudson.plugins.sonar.SonarPublisher' {
                    jdk(SONAR_JDK_VERSION)
                    installationName('https://sonar.epk.ericsson.se/')
                    branch()
                    language()
                    mavenInstallationName(mavenInstallation)
                    if (mavenProjectLocation) {
                        rootPom(mavenProjectLocation)
                    }
                    mavenOpts(MAVEN_OPTS)
                    jobAdditionalProperties(sonarAdditionalProperties)
                    settings(class: 'jenkins.mvn.DefaultSettingsProvider')
                    globalSettings(class: 'jenkins.mvn.DefaultGlobalSettingsProvider')
                    usePrivateRepository(false)
                }
            }
        }
    }

    protected String getSonarAdditionalProperties() {
        String sonarParameters =  ""

        if (profilesToBeUsed != null && profilesToBeUsed != "") {
            sonarParameters += ' -P' + profilesToBeUsed + ' '
        }

        if (generateCoberturaReport){
            sonarParameters += '-Dsonar.core.codeCoveragePlugin="cobertura" '
        }
        else{
            sonarParameters += '-Dsonar.core.codeCoveragePlugin="jacoco" '
        }

        if (branchName != 'master') {
            sonarParameters += '-Dsonar.branch=' +  branchName.replace('/', '_') + ' '
        }

        sonarParameters += '-Dsonar.analysis.mode=analysis -Dsonar.profile="' + sonarProfile + '" ' +
                '-DsingleThreadTest '

        sonarParameters += MAVEN_PARAMETERS

        return sonarParameters
    }
}
