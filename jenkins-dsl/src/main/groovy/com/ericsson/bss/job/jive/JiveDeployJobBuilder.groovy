package com.ericsson.bss.job.jive

import com.ericsson.bss.job.DeployJobBuilder

class JiveDeployJobBuilder extends DeployJobBuilder{

    String jiveTeamMail = "PDLJBTFDEV@ex1.eemea.ericsson.se"

    List<String> quickReleaseProjects = ["jive-protocols"]

    String moduleName = ""
    @Override
    protected void addDeployConfig() {
        moduleName = mavenProjectLocation
        job.with {
            if (useQuickRelease()) {
                String QUICK_RELEASE_DESCRIPTION = "" +
                        "<i><h2>Release will be handled differently with this job</h2></i>\n" +
                        "<p>When doing a release, it will update-properties and update-parent to " +
                        "latest release artifacts. </p><p>And after the release it will set the " +
                        "latest " +
                        "snapshot and deploy the new snapshot artifact</p><br>"
                description(DSL_DESCRIPTION + QUICK_RELEASE_DESCRIPTION + JOB_DESCRIPTION)
            }else{
                description(DSL_DESCRIPTION + JOB_DESCRIPTION)
            }
            addGitRepository(gerritName, branch)
            triggers {
                snapshotDependencies(true)
            }
            getDeployConfig()
            if (prepareArm2Gask) {
                addPrepareArm2Gask()
            }
            addBlameMail()

            publishers { wsCleanup() }
        }
    }

    protected getDeployConfig() {
        workspacePath
        def preSteps = {
            shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
            shell(removeOldArtifacts())
            shell(gitConfig("\${WORKSPACE}"))
            if (generateGUIconfig) {
                shell(gconfWorkspaceWorkaround())
            }

            if (useQuickRelease()){
                shell(addPreReleaseSteps())
            }
        }

        Map environmentVariables = getInjectVariables()

        job.with {
            preBuildSteps(preSteps)

            injectEnv(environmentVariables)

            addMavenConfig()
            if (workspacePath != null && !workspacePath.equals("")) {
                customWorkspace(workspacePath)
            }
            addMavenRelease()
            addTimeoutConfig()

            if (useQuickRelease()) {
                addPostReleaseSteps()
            }
        }
    }

    private Boolean useQuickRelease(){
        String compareMe = moduleName
        if (compareMe.equals("")){
            compareMe = "jive-core"
        }

        for (project in quickReleaseProjects){
            if (compareMe.contains(project))
                return true
        }
        return false

    }

    private String addPreReleaseSteps(){
        String mvn = "mvn"
        if (mavenProjectLocation){
            mvn += " -f " + mavenProjectLocation
        }
        String mavenGeneralBuildParameters = getMavenGeneralBuildParameters().replaceAll("\\\\\n", "")
        String gitURL = "ssh://" + gerritServer + ":29418/" + gerritName + ".git"
        return "" +
                getShellCommentDescription("PREPARE RELEASE") +
                "if [ -n \"\$MVN_RELEASE_VERSION\" ]; then\n" +
                "  # Update parent and properties to latest release \n"+
                "  " + mvn + " org.codehaus.mojo:versions-maven-plugin:2.3-E001:update-parent -DgenerateBackupPoms=false -U -DallowSnapshots=false " +
                mavenGeneralBuildParameters + "\n" +
                "  " + mvn + " org.codehaus.mojo:versions-maven-plugin:2.3-E001:update-properties -DgenerateBackupPoms=false -U -DallowSnapshots=false " +
                mavenGeneralBuildParameters + "\n" +
                "  git add -u\n" +
                "  git commit -m \"Prepare for release\"\n" +
                "  git push " + gitURL + " HEAD:refs/heads/" + branch + "\n" +
                "fi"
    }

    private String addPostReleaseSteps(){
        String mvn = "mvn"
        if (mavenProjectLocation){
            mvn += " -f " + mavenProjectLocation
        }

        String gitURL = "ssh://" + gerritServer + ":29418/" + gerritName + ".git"
        String mavenGeneralBuildParameters = getMavenGeneralBuildParameters().replaceAll("\\\\\n", "")
        def commandRelease = "" +
                getShellCommentDescription("POST RELEASE") +
                "if [ -n \"\$MVN_RELEASE_VERSION\" ]; then\n" +
                "  # Update parent and properties to latest snapshot \n"+
                "  " + mvn + " org.codehaus.mojo:versions-maven-plugin:2.3-E001:update-parent -DgenerateBackupPoms=false -DallowSnapshots=true " +
                "-U " + mavenGeneralBuildParameters + "\n" +
                "  " + mvn + " org.codehaus.mojo:versions-maven-plugin:2.3-E001:update-properties -DgenerateBackupPoms=false " +
                "-DallowSnapshots=true -U " + mavenGeneralBuildParameters + "\n" +
                "  if [ \"\$MVN_ISDRYRUN\"  == \"false\" ]; then\n" +
                "    git add -u\n" +
                "    git commit -m \"Prepare for next development version\"\n" +
                "    git push " + gitURL + " HEAD:refs/heads/" + branch + "\n" +
                "  fi\n" +
                "fi"

        def commandDeploySnapshotAfterRelease = "" +
                getShellCommentDescription("POST RELEASE DEPLOY SNAPSHOT") +
                "if [ \"\$MVN_ISDRYRUN\"  == \"false\" ]; then\n" +
                "  # Deploy latest snapshot\n"+
                "  " + mvn + " " + getMvnGoal() + " \n" +
                "fi"

        job.with {
            postBuildSteps {
                conditionalSteps {
                    condition{
                        status('SUCCESS', 'SUCCESS')
                    }
                    runner('DontRun')
                    steps {
                        shell(commandRelease)
                        shell(commandDeploySnapshotAfterRelease)
                    }
                }
            }
        }
    }

    @Override
    protected void addExtendableEmail() {
        def emailTrigger = {
            email {
                recipientList '$PROJECT_DEFAULT_RECIPIENTS'
                subject '$PROJECT_DEFAULT_SUBJECT'
                body '$PROJECT_DEFAULT_CONTENT'
                recipientProviders {
                    'hudson.plugins.emailext.plugins.recipients.ListRecipientProvider' {}
                    'hudson.plugins.emailext.plugins.recipients.CulpritsRecipientProvider' {}
                    'hudson.plugins.emailext.plugins.recipients.DevelopersRecipientProvider' {}
                    'hudson.plugins.emailext.plugins.recipients.RequesterRecipientProvider' {}
                    'hudson.plugins.emailext.plugins.recipients.FailingTestSuspectsRecipientProvider' {}
                    'hudson.plugins.emailext.plugins.recipients.UpstreamComitterRecipientProvider' {}
                }
                'attachmentsPattern' {}
                'attachBuildLog' 'false'
                'compressBuildLog' 'false'
                'replyTo' '$PROJECT_DEFAULT_REPLYTO'
                'contentType' 'project'
            }
        }

        job.with {
            configure { project ->
                project / publishers << 'hudson.plugins.emailext.ExtendedEmailPublisher' {
                    recipientList '$DEFAULT_RECIPIENTS' + ', cc:' + jiveTeamMail
                    configuredTriggers {
                        'hudson.plugins.emailext.plugins.trigger.FailureTrigger' emailTrigger
                        'hudson.plugins.emailext.plugins.trigger.UnstableTrigger' emailTrigger
                        'hudson.plugins.emailext.plugins.trigger.AbortedTrigger' emailTrigger
                    }
                    contentType 'text/html'
                    defaultSubject '$DEFAULT_SUBJECT'
                    defaultContent '${SCRIPT, template="email_with_upstream_changes.template"}'
                    'attachmentsPattern' {}
                    'presendScript' '$DEFAULT_PRESEND_SCRIPT'
                    'attachBuildLog' 'false'
                    'compressBuildLog' 'false'
                    'replyTo' '$DEFAULT_REPLYTO'
                    'saveOutput' 'false'
                    'disabled' 'false'
                }
            }
        }
    }
}
