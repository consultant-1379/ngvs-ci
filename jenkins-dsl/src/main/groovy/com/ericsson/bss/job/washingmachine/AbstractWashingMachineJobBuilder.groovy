package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.util.Email
import javaposse.jobdsl.dsl.Job

abstract class AbstractWashingMachineJobBuilder extends AbstractJobBuilder {

    protected static final String WASHINGMACHINE_SUFFIX = '_washingmachine'
    protected static final String RPM_SUFFIX = '_rpm'

    protected static final int DAYS_TO_KEEP_BUILDS = 20
    protected static final int MAX_BUILDS_TO_KEEP = 20
    protected static final int DEFAULT_TIMEOUT = 240
    protected static final int DEFAULT_TAPAS_TIMEOUT = (DEFAULT_TIMEOUT-10)*60

    protected static final String EMAIL_RECIPIENT = 'recipient'
    protected static final String EMAIL_SUBJECT = 'subject'
    protected static final String EMAIL_CONTENT = 'content'
    protected static final String EMAIL_FAILURE_TRIGGER_SUBJECT = 'failureTriggerSubject'
    protected static final String EMAIL_FIXED_TRIGGER_SUBJECT = 'fixedTriggerSubject'
    protected static final String EMAIL_UNSTABLE_TRIGGER_SUBJECT = 'unstableTriggerSubject'
    protected static final String EMAIL_ABORTED_TRIGGER_SUBJECT = 'abortedTriggerSubject'

    protected def out
    protected String projectName
    protected Map<String, String> mailConfig

    public Job build() {
        jobName = getProjectName()
        out.println("Creating " + jobName)
        initProject(dslFactory.freeStyleJob(jobName))

        setRestrictLabel()
        addProjectConfig()
        addBuildSteps()
        addPostBuildActions()

        return job
    }

    protected String getProjectName() {
        return projectName + WASHINGMACHINE_SUFFIX
    }

    protected String getProjectDescription() {
        return 'Installs latest ' + projectName.capitalize() + ' continuously to verify that everything works.'
    }

    protected void addPostBuildScripts() {
        job.with {
            publishers {
                postBuildScripts {
                    steps {
                        environmentVariables {
                            propertiesFile('env.properties')
                        }
                    }
                    onlyIfBuildSucceeds(false)
                }
            }
        }
    }

    protected void prepareMailConfig() {
    }

    protected boolean hasEmailNotification() {
        return true
    }

    protected void addSpecificConfig() {
    }

    protected void addPreScriptBuildSteps() {
    }

    protected void addPostScriptBuildSteps() {
    }

    protected void addArchiveArtifactsConfig() {
    }

    protected void addTriggerParameterizedBuildOnOtherProjectsConfig() {
    }

    protected void addProjectConfig() {
        addProjectBasicConfig()
        addTimeoutAndAbortConfig(getWashingMachineTimeout())
        deleteWorkspaceBeforeBuildStarts()
        setJenkinsUserBuildVariables()
        addSpecificConfig()
    }

    protected int getWashingMachineTimeout() {
        return DEFAULT_TIMEOUT
    }

    protected void addProjectBasicConfig() {
        job.with {
            description(getProjectDescription())
            logRotator(DAYS_TO_KEEP_BUILDS, MAX_BUILDS_TO_KEEP)
        }
    }

    protected void addBuildSteps() {
        addPreScriptBuildSteps()
        addScriptBuildStep()
        addPostScriptBuildSteps()
    }

    protected void addScriptBuildStep() {
        job.with {
            steps {
                shell(dslFactory.readFileFromWorkspace(getBuildScriptName()).replaceAll('killtimeout', DEFAULT_TAPAS_TIMEOUT.toString()))
            }
        }
    }

    protected String getBuildScriptName() {
        return "scripts/washingmachine/" + jobName + ".sh"
    }

    protected void addPostBuildActions() {
        addArchiveArtifactsConfig()
        addPostBuildBasicConfig()
        if (hasEmailNotification()) {
            configureEmailNotification()
        }
        addTriggerParameterizedBuildOnOtherProjectsConfig()
        if (jobName.contains("cil") || jobName.contains("invoicing") || jobName.contains("coba") ) {
            addPostWorkspaceCleanup()
        }
    }

    private void addPostBuildBasicConfig() {
        job.with {
            publishers {
                textFinder('^.*Reporting end of session with status 2.*$', '', true, false, true)
                buildDescription('^.*JENKINS_DESCRIPTION(.*)', '', '^.*JENKINS_DESCRIPTION(.*)', '')
            }
        }
        addPostBuildScripts()
    }

    private void addPostWorkspaceCleanup() {
        job.with {
            publishers {
                wsCleanup()
            }
        }
    }

    private void configureEmailNotification() {
        mailConfig = new HashMap()
        prepareMailConfig()

        Email email = Email.newBuilder().withRecipient(mailConfig.get(EMAIL_RECIPIENT))
                .withSubject(mailConfig.get(EMAIL_SUBJECT))
                .withContent(mailConfig.get(EMAIL_CONTENT))
                .withFailureTrigger(mailConfig.get(EMAIL_FAILURE_TRIGGER_SUBJECT))
                .withFixedTrigger(mailConfig.get(EMAIL_FIXED_TRIGGER_SUBJECT))
                .withUnstableTrigger(mailConfig.get(EMAIL_UNSTABLE_TRIGGER_SUBJECT))
                .withAbortedTrigger(mailConfig.get(EMAIL_ABORTED_TRIGGER_SUBJECT))
                .build()

        addEmailNotificationConfig(email)
    }
}
