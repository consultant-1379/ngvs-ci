package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.util.Email
import javaposse.jobdsl.dsl.Job

public class WashingMachineReleaseBranchOnOffJobBuilder extends AbstractJobBuilder {

    private out
    private projectName

    private Object releaseBranch

    private String recipient
    private String releaseBranchName
    private String washingmachineReleasebranchSuffix = '_washingmachine_releasebranch'
    private String rpmString = ""

    protected boolean isRpm = false

    public Job build() {
        if (isRpm) {
            washingmachineReleasebranchSuffix += "_rpm"
            rpmString = "Rpm "
        }
        releaseBranchName = releaseBranch.@releasebranchname
        jobName = getProjectName()
        out.println("Creating " + jobName + " for releasebranch" + releaseBranchName)
        initProject(dslFactory.freeStyleJob(jobName))
        setProjectDescription(getProjectDescription())
        setJenkinsUserBuildVariables()
        addBuildParametersConfig()
        addBuildSteps()
        setBuildDescription('^.*With description: (.*)')
        super.configurePostBuildSteps().editableEmailNotification(getAlwaysMailConfig())
        return job
    }

    private void addBuildSteps() {
        job.with {
            steps {
                systemGroovyCommand(getGroovyCommand())
            }
        }
    }

    private String getProjectDescription() {
        return 'A job to enable and disable ' + projectName.capitalize() + ' Washingmachine Releasebranch ' + rpmString + releaseBranchName +
               ' to allow troubleshooting on targethosts.'
    }

    private String getProjectName() {
        return projectName + washingmachineReleasebranchSuffix + "_" + releaseBranchName + "_onoff"
    }

    private String getProjectWithoutSuffix() {
        return projectName + washingmachineReleasebranchSuffix + "_" + releaseBranchName
    }

    private String getProjectKeepaliveProjectName() {
        return projectName + washingmachineReleasebranchSuffix + "_" + releaseBranchName + "_keepalive"
    }

    protected String getActionScript() {
        return dslFactory.readFileFromWorkspace('scripts/washingmachine/job_enabled_param.groovy')
            .replace("<PROJECT_NAME>", getProjectWithoutSuffix())
    }

    protected void addBuildParametersConfig() {
        job.with {
            parameters {
                activeChoiceParam("JOB_ENABLED") {
                    description("Choose to enable or disable " + getProjectWithoutSuffix())
                    choiceType("SINGLE_SELECT")
                    groovyScript {
                        script(getActionScript())
                        fallbackScript(defaultFallbackScript())
                    }
                }
                stringParam('REASON', '', 'The reason why you are changing state of ' +
                    getProjectWithoutSuffix().capitalize() + ' WashineMachine.')
            }
        }
    }

    protected String getGroovyCommand() {
        String shell = "import jenkins.model.Jenkins\n" +
                       "import jenkins.model.*\n" +
                       "import hudson.model.*\n" +
                       "Hudson instance = Jenkins.getInstance()\n" +
                       "String reason = build.buildVariableResolver.resolve('REASON')\n"+
                       "String user = build.getEnvironment(listener).get('BUILD_USER_ID')\n" +
                       "String jobname = '" + getProjectKeepaliveProjectName() + "'\n" +
                       "FreeStyleProject job = instance.getItem(jobname)\n"
        if (!isRpm) {
            shell +=   "String parentJobname = '" + getProjectWithoutSuffix() + "'\n" +
                       "FreeStyleProject parentJob = instance.getItem(parentJobname)\n"
        }
        shell +=       "String jobEnabled = build.buildVariableResolver.resolve('JOB_ENABLED')\n" +
                       "\n" +
                       "if (jobEnabled == 'Disable') {\n" +
                           "\tprintln 'Disabling ' + jobname\n" +
                           "\tdesc = 'Job disabled by ' + user + ' with reason: ' + reason + ''\n" +
                           "\tprintln 'With description: ' + desc\n" +
                           "\tjob.disable()\n"
       if (!isRpm) {
           shell +=        "\tparentJob.disable()\n"
       }
       shell +=            "\tjob.setDescription(desc)\n" +
                       "} else {\n" +
                           "\tdef hostSetDescription = 'all hosts'\n" +
                           "\n" +
                           "\tprintln 'Enabling ' + jobname\n" +
                           "\tdesc = 'Job enabled (for ' + hostSetDescription + ') by ' + user + ' with reason: ' + reason + ''\n" +
                           "\tprintln 'With description: ' + desc\n" +
                           "\tjob.enable()\n"
       if (!isRpm) {
           shell +=        "\tparentJob.enable()\n"
       }
       shell +=           "\tjob.setDescription(desc)\n" +
                       "}\n" +
                       "prepareParams(jobEnabled == 'Disable' ? false : true)\n" +
                       "\n" +
                       "private void prepareParams(boolean jobEnabled) {\n" +
                           "\tProperties props = new Properties()\n" +
                           "\tFile propsFile = new File('" + PATH_TO_JOB_CONFIG + projectName + "_washingmachine_releasebranch_" +
                                                        releaseBranchName + "_params.properties')\n" +
                           "\tif ( !propsFile.exists() ) {\n" +
                               "\t\t//Create new file\n" +
                               "\t\tpropsFile.createNewFile()\n" +
                           "\t}\n" +
                           "\tprops.load(propsFile.newDataInputStream())\n" +
                           "\tprops.setProperty('JOB_ENABLED', Boolean.toString(jobEnabled))\n" +
                           "\tprops.store(propsFile.newWriter(), null)\n" +
                           "\tprintln 'Content of the params file:'\n" +
                           "\tprintln propsFile.text\n" +
                       "}"
        return shell
    }

    protected String defaultFallbackScript() {
        return 'return ["Error evaluating Groovy script."]'
    }

    protected Email getAlwaysMailConfig() {
        return Email.newBuilder().withRecipient(recipient)
                .withSubject(getSubject())
                .withContent(getContent())
                .withAlwaysTrigger()
                .build()
    }

    private String getSubject() {
        return projectName.capitalize() + " WashingMachine Releasebranch " + rpmString + releaseBranchName + " state changed to \$JOB_ENABLED"
    }

    private String getContent () {
        return projectName.capitalize() + " WashingMachine Releasebranch " + rpmString + releaseBranchName +
                " state changed to \$JOB_ENABLED by \$BUILD_USER_ID with reason '\$REASON'"
    }
}
