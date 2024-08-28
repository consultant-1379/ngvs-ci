package com.ericsson.bss.job

import static com.ericsson.bss.util.scriptbuilders.SonarReviewPosterScriptBuilder.DEFAULT_GERRIT_USER
import static com.ericsson.bss.util.scriptbuilders.SonarReviewPosterScriptBuilder.DEFAULT_RELATIVE_SONAR_REPORT_PATH
import static com.ericsson.bss.util.scriptbuilders.SonarReviewPosterScriptBuilder.getScript

import com.ericsson.bss.AbstractGerritJobBuilder
import com.ericsson.bss.Project
import javaposse.jobdsl.dsl.Job

public class GerritSonarJobBuilder extends AbstractGerritJobBuilder {
    protected final static String JOB_DESCRIPTION  = "<h2>Job that does code analyze on Gerrit reviews.</h2>\n" +
            "<p>The job automatically starts when a new review is published in Gerrit.</p>\n" +
            "<ul>\n" +
            "  <li>It verifies that the commit message is written according to the " +
            "<a href='https://eta.epk.ericsson.se/maven-sites/latest/com.ericsson.bss.rm.charging/parent/site/git.html#Commit_Message'>" +
            "recommendations</a>.</li>\n" +
            "  <li>It runs an incremental SonarQube analyze on the changed code in the review.</li>\n" +
            "</ul>\n"  +
            "Related questions\n" +
            "<ul>\n" +
            "  <li><a href=\"https://eqna.lmera.ericsson.se/questions/32718\">Who is responsible for coding convention?</a></li>\n" +
            "<ul>\n" +
            BSSF_MAVEN_CI_DESCRIPTION

    protected boolean generateCoberturaReport
    protected String verifyCommitMessage = 'scripts/VerifyCommitMessage.groovy'
    protected String sonarProfile = "BMARP Design Rules"
    protected String branchName = "master"
    protected String sonarMavenPlugin = "org.sonarsource.scanner.maven:sonar-maven-plugin:3.0.2"

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))

        addGerritSonarConfig()
        super.gerritTriggerSilent(branchName)
        return job
    }

    protected void addGerritSonarConfig() {
        job.with {

            description(JOB_DESCRIPTION)
            concurrentBuild()
            injectEnv(getInjectVariables())
            addTimeoutConfig()
        }
        addJobs()
    }

    protected void addJobs() {
        job.with {
            steps {
                shell(gitInitiateRepository("\${WORKSPACE}"))
                shell(gitConfig("\${WORKSPACE}"))
                shell(getGitCache("\${WORKSPACE}"))
                shell(gitFetchGerritChange())
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
                groovyCommand(verifyCommitMessageScript(), GROOVY_INSTALLATION_NAME)
                shell(gerritReportCommitMessage())
                shell(removeOldArtifacts())
                if (generateGUIconfig) {
                    shell(gconfWorkspaceWorkaround())
                }
                shell(getCoverageCommand())
                shell(SONAR_ANALYSIS_LOG_OUTPUT)
                if (generateGUIconfig) {
                    shell(getCDTSonarTesReportWorkaround())
                }
                shell(getMavenSonarCommand())
                String mavenProjectFolder = mavenProjectLocation && mavenProjectLocation.length() > POM_XML.length() ?
                        mavenProjectLocation[0..-POM_XML.length()]:""
                shell(getSonarPosterCommand(mavenProjectFolder))
                if (mavenProjectFolder) {
                    shell("# Copy the issues-report " +
                            "\nmkdir -p target/sonar/issues-report ; cp -r " + mavenProjectFolder +
                            "/target/sonar/issues-report/* target/sonar/issues-report")
                }
                shell(copySitePreview("./target/sonar/issues-report/*", "sonar", projectName))
                shell(grepSonarIssues())
            }
        }
    }

    protected String getSonarPosterCommand(String mavenProjectFolder) {
        String cmd = getShellCommentDescription("Sonar review poster")

        if (isUsingLabelForReviews()) {
            cmd += getScript(mavenProjectFolder, DEFAULT_RELATIVE_SONAR_REPORT_PATH, gerritServer, Project.GERRIT_EPK_USER)
        }
        else{
            cmd += getScript(mavenProjectFolder, DEFAULT_RELATIVE_SONAR_REPORT_PATH, gerritServer, DEFAULT_GERRIT_USER)
        }

        return cmd
    }

    protected String getCoverageCommand() {
        String mavenSubProjectCmd = ""
        if (mavenProjectLocation){
            mavenSubProjectCmd = " -f " + mavenProjectLocation
        }
        String cmd = getShellCommentDescription("Command to generate coverage report") +
                "mvn " + mavenSubProjectCmd +  " \\\n" +
                "clean "

        if (generateCoberturaReport) {
            cmd += 'cobertura:cobertura -Dcobertura.report.format=xml '
        }
        else {
            cmd += JACOCO_AGENT + ' '
        }

        cmd += "install \\\n"

        if (runParallelThreads || generateCoberturaReport) {
            cmd += "-DparallelTests -T 4 \\\n"
        }
        cmd += "-Dsurefire.useFile=false \\\n" +
                getMavenGeneralBuildParameters() + " \\\n" +
                "> console_out.txt"

        return cmd
    }

    protected static final String SONAR_ANALYSIS_LOG_OUTPUT = "echo \"----------------------------\" >> console_out.txt\n" +
    "echo \"-                          -\" >> console_out.txt\n" +
    "echo \"-  Sonar Analysis started  -\" >> console_out.txt\n" +
    "echo \"-                          -\" >> console_out.txt\n" +
    "echo \"----------------------------\" >> console_out.txt\n"

    protected String getMavenSonarCommand() {
        String mavenSubProjectCmd = ""
        if (mavenProjectLocation){
            mavenSubProjectCmd = " -f " + mavenProjectLocation
        }
        String cmd = getShellCommentDescription("Sonar incremental analysis")
        if (!isMasterBranch() && branchName.contains('/')) {
            cmd += "git branch -f " + branchName.replace('/', '_') + '\n'
        }
        cmd += "mvn " + mavenSubProjectCmd + " \\\n" +
                sonarMavenPlugin + ":sonar -Dsonar.issuesReport.console.enable=true -Dsonar.issuesReport.html.enable=true " +
                "-Dsonar.analysis.mode=incremental -Dsonar.profile=\"" + sonarProfile + "\" \\\n"

        if (generateCoberturaReport) {
            cmd += '-Dsonar.core.codeCoveragePlugin="cobertura" '
        } else {
            cmd += '-Dsonar.core.codeCoveragePlugin="jacoco" '
        }

        if (!isMasterBranch()) {
            cmd += '-Dsonar.branch=' + branchName.replace('/', '_') + ' '
        }

        cmd += "-Dsonar.host.url=https://sonar.epk.ericsson.se \\\n" +
                "-Dsurefire.useFile=false \\\n" +
                getMavenGeneralBuildParameters() + " \\\n" +
                ">> console_out.txt"
        return cmd
    }
    protected String grepSonarIssues(){
        return getShellCommentDescription("Fail the job if new sonar issues found.") +
                "grep \"No new issue\" console_out.txt"
    }

    @Override
    protected void setPublishers() {
        job.with {
            publishers {
                flexiblePublish {
                    conditionalAction {
                        condition { alwaysRun() }
                        steps { shell('cat console_out.txt') }
                    }
                }
            }
        }
        superSetPublishers()
    }

    protected void superSetPublishers(){
        super.setPublishers()
    }

    protected String gerritReportCommitMessage() {
        String cmd = 'ssh -o BatchMode=yes -p 29418 -l ${GERRIT_USER} ' + gerritServer + ' gerrit review --project \${GERRIT_PROJECT} ' +
                getNotifySetting() + '-m \'"Recommendation for git commit message\n' +
                '\'"\${VERIFY_COMMIT}"\'"\' \${GERRIT_PATCHSET_REVISION}'
        cmd += addRetry(cmd)

        cmd = getShellCommentDescription("If commit message not follow standards") +
                addGerritUser(jenkinsCodeQualityUser) +
                'if [ -f commitverify.txt ] ; then\n' +
                '  VERIFY_COMMIT=\$(cat commitverify.txt)\n' +
                '  if [[ ! -z "\${VERIFY_COMMIT}" ]] ; then\n' +
                '    ' + cmd + '\n' +
                '  fi\n' +
                'fi'

        return cmd
    }

    @Override
    protected String gerritFeedbackFail() {
        String cmd = "ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT} " + getNotifySetting() + "-m '\"Sonar preview: " +
                getSitePreviewUrl("sonar", projectName) + "'\${GERRIT_PATCHSET_REVISION}'/target/sonar/issues-report/issues-report.html \"' " +
                addGerritLabel("\${CODE_REVIEW}") + " \${GERRIT_PATCHSET_REVISION}"
        cmd += addRetry(cmd)

        String codeReview = "CODE_REVIEW=0\n" +
                "if tac console_out.txt | grep -m 1 -q \"\\+[0-9]* issue\" console_out.txt ; then\n" +
                "CODE_REVIEW=-1\n" +
                "elif tac console_out.txt | grep -m 1 -q \"No new issue\" console_out.txt ; then\n" +
                "CODE_REVIEW=1\n" +
                "fi\n"

        return codeReview +
                addGerritUser(jenkinsCodeQualityUser) +
                "if [ -f \"target/sonar/issues-report/issues-report.html\" ]; then \n" +
                "  echo \"Sonar preview: " +
                getSitePreviewUrl("sonar", projectName) + "'\${GERRIT_PATCHSET_REVISION}'/target/sonar/issues-report/issues-report.html \"\n" +
                "   " + cmd + "\n" +
                "fi"
    }

    @Override
    protected String gerritFeedbackSuccess(boolean verboseGerritFeedback) {
        String cmd = addGerritUser(jenkinsCodeQualityUser) +
                "if [ -f \"target/sonar/issues-report/issues-report.html\" ]; then \n" +
                "  echo \"Sonar preview: " +
                getSitePreviewUrl("sonar", projectName) + "'\${GERRIT_PATCHSET_REVISION}'/target/sonar/issues-report/issues-report.html \"\n"

        String reviewCmd = "  ssh -o BatchMode=yes -p 29418 -l \${GERRIT_USER} " + gerritServer +
                " gerrit review --project \${GERRIT_PROJECT} " + getNotifySetting()

        if (verboseGerritFeedback) {
            reviewCmd += " -m '\"No new issues found related to this commit.\n" +
                    "To view existing issues, deselect \\\"Only NEW issues\\\" in the report.\n" +
                    getSitePreviewUrl("sonar", projectName) + "'\${GERRIT_PATCHSET_REVISION}'/target/sonar/issues-report/issues-report.html \"' " +
                    addGerritLabel("1") + " \${GERRIT_PATCHSET_REVISION}"
        }
        else{
            reviewCmd += "  " + addGerritLabel("0") + " \${GERRIT_PATCHSET_REVISION}"
        }
        reviewCmd += addRetry(reviewCmd) + "\nfi"

        return cmd + reviewCmd
    }

    protected String addGerritLabel(String feedbackValue) {
        if (isUsingLabelForReviews()) {
            "--label Code-Quality=" + feedbackValue
        }
        else {
            "-l Code-Review=" + feedbackValue
        }
    }

    protected String verifyCommitMessageScript() {
        if (verifyCommitMessage == "") {
            return ""
        }
        else {
            return dslFactory.readFileFromWorkspace(verifyCommitMessage)
        }
    }

    protected boolean isMasterBranch() {
        return branchName == 'master'
    }

    protected String grepCommitMessageIssues(){
        return getShellCommentDescription("Fail the job if Tracking-Id issues found in commit-message.") +
                "grep \"No issue found\" trackIdVerify.txt"
    }
}
