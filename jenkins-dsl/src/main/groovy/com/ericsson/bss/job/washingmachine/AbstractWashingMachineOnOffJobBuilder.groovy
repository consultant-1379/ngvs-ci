package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.job.washingmachine.utils.WashingMachineConstantsHelper
import com.ericsson.bss.job.washingmachine.utils.WashingMachineOnOffScriptsBuilder
import com.ericsson.bss.util.Email
import javaposse.jobdsl.dsl.Job

@Deprecated
abstract class AbstractWashingMachineOnOffJobBuilder extends AbstractJobBuilder {

    protected def out
    protected String projectName
    protected Map<String, String> mailConfig

    protected abstract String getProjectDescription()
    protected abstract String getProjectToBuildName()
    protected abstract void prepareMailConfig()

    public Job build() {
        jobName = getJobName()
        out.println("Creating " + jobName)
        this.job = dslFactory.freeStyleJob(jobName)
        setRestrictLabel()
        setJDK()
        addBuildParameters()
        setProjectDescription(getProjectDescription())
        setJenkinsUserBuildVariables()
        addBuildSteps()
        setBuildDescription('^.*With description: (.*)')
        configureEmailNotification()

        return job
    }

    protected String getJobName() {
        return getProjectToBuildName() + WashingMachineConstantsHelper.ON_OFF_SUFFIX
    }

    protected void addBuildParameters() {
        job.with {
            parameters {
                activeChoiceParam("ACTION") {
                    description("Choose action to enable och disable " + getProjectToBuildName().capitalize() + " WashingMachine.")
                    choiceType("SINGLE_SELECT")
                    groovyScript {
                        script(getActionScript())
                        fallbackScript(defaultFallbackScript())
                    }
                }
                stringParam('REASON', '', 'The reason why you are changing state of ' + getProjectToBuildName().capitalize() + ' WashineMachine.')
            }
        }
    }

    protected String getActionScript() {
        return dslFactory.readFileFromWorkspace('scripts/washingmachine/job_enabled_param.groovy')
               .replace("<PROJECT_NAME>", getProjectToBuildName())
    }

    protected String defaultFallbackScript() {
        return 'return ["Error evaluating Groovy script."]'
    }

    private void addBuildSteps() {
        job.with {
            steps {
                systemGroovyCommand(
                        WashingMachineOnOffScriptsBuilder.newBuilder(dslFactory, getProjectToBuildName()).build()
                )
            }
        }
    }

    private void configureEmailNotification() {
        mailConfig = new HashMap()
        prepareMailConfig()

        Email email = Email.newBuilder().withRecipient(mailConfig.get(AbstractWashingMachineJobBuilder.EMAIL_RECIPIENT))
                .withSubject(mailConfig.get(AbstractWashingMachineJobBuilder.EMAIL_SUBJECT))
                .withContent(mailConfig.get(AbstractWashingMachineJobBuilder.EMAIL_CONTENT))
                .withAlwaysTrigger()
                .build()

        addEmailNotificationConfig(email)
    }
}
