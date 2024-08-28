package com.ericsson.bss.decorators

import com.ericsson.bss.util.Email
import javaposse.jobdsl.dsl.Job

final class ConfigurablePostBuildStepsDecorator extends PostBuildStepsDecorator<ConfigurablePostBuildStepsDecorator> {

    private ConfigObject config

    ConfigurablePostBuildStepsDecorator(ConfigObject config, Job job) {
        super(job)
        this.config = config
    }

    ConfigurablePostBuildStepsDecorator editableEmailNotificationFromConfig() {
        editableEmailNotification(mailConfiguration(config))
        return this
    }

    ConfigurablePostBuildStepsDecorator executeASetOfScriptsFromConfig(boolean _onlyIfBuildSucceeds = false) {
        job.with {
            publishers {
                postBuildScripts {
                    onlyIfBuildSucceeds(_onlyIfBuildSucceeds)
                    steps {
                        environmentVariables {
                            propertiesFile('env.properties')
                        }
                        if (config.projectsToTrigger.projects) {
                            downstreamParameterized {
                                trigger(config.projectsToTrigger.projects.join(',')) {
                                    parameters CommonParamsDecorator.addParameters(config.projectsToTrigger.parameters)
                                }
                            }
                        }
                    }
                }
            }
        }
        return this
    }

    /**
     * Triggers parameterized builds on other projects. (SUCCESS,FAILED_OR_BETTER,UNSTABLE,ALWAYS,UNSTABLE_OR_WORSE,UNSTABLE_OR_BETTER,FAILED)
     * @param projects Coma separated project names.
     * @param conditions Configuration for triggered build.
     * @return
     */
    ConfigurablePostBuildStepsDecorator triggerParameterizedBuildOnOtherProjectsFromConfig() {
        job.with {
            publishers {
                downstreamParameterized {

                    for (i in 0..config.triggerParameterizedBuildOnOtherProjectsConfig.conditions.size() - 1) {
                        trigger(config.triggerParameterizedBuildOnOtherProjectsConfig.projects) {
                            condition(config.triggerParameterizedBuildOnOtherProjectsConfig.conditions[i])
                            parameters CommonParamsDecorator.addParameters(
                                    config.triggerParameterizedBuildOnOtherProjectsConfig.conditions[i],
                                    config.triggerParameterizedBuildOnOtherProjectsConfig.parameters
                            )
                        }
                    }

                }
            }
        }
        return this
    }

    private Email mailConfiguration(ConfigObject config) {
        return Email.newBuilder().withRecipient(config.editableEmailNotification.recipient)
                .withSubject(config.editableEmailNotification.subject)
                .withContent(config.editableEmailNotification.content)
                .withFailureTrigger(config.editableEmailNotification.failureTriggerSubject)
                .withFixedTrigger(config.editableEmailNotification.fixedTriggerSubject)
                .withUnstableTrigger(config.editableEmailNotification.unstableTriggerSubject)
                .withAbortedTrigger(config.editableEmailNotification.abortedTriggerSubject)
                .withAlwaysTrigger(config.editableEmailNotification.alwaysTriggerSubject)
                .build()
    }

    ConfigurablePostBuildStepsDecorator emailNotificationFromConfig() {
        job.with {
            publishers {
                mailer(
                        config.emailNotification.recipients.join(' '),
                        config.emailNotification.dontNotifyEveryUnstableBuild ?: false,
                        config.emailNotification.sendToIndividuals ?: false
                )
            }
        }
        return this
    }
}
