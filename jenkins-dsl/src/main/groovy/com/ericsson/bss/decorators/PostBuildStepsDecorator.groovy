package com.ericsson.bss.decorators

import com.ericsson.bss.util.Email
import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.helpers.publisher.PublisherContext.Behavior

class PostBuildStepsDecorator<T extends PostBuildStepsDecorator> {

    protected Job job

    PostBuildStepsDecorator(Job job) {
        this.job = job
    }

    /**
     * Sends email notifications.
     * @param recipients
     * @param dontNotifyEveryUnstableBuild
     * @param sendToIndividuals
     * @return
     */
    T emailNotification(String recipients, Boolean dontNotifyEveryUnstableBuild, Boolean sendToIndividuals) {
        job.with {
            publishers {
                mailer(recipients, dontNotifyEveryUnstableBuild, sendToIndividuals)
            }
        }
        return this
    }

    /**
     * Archives artifacts with each build.
     * @param patterns Specifies the files to archive.
     * @param _allowEmpty If set, does not fail the build if archiving returns nothing. Defaults to false.
     * @param _onlyIfSuccessful Archives artifacts only if the build is successful. Defaults to false.
     * @param _fingerprint Fingerprints all archived artifacts. Defaults to false.
     * @param _defaultExcludes Uses default excludes. Defaults to true.
     * @return
     */
    T archiveTheArtifacts(List<String> patterns, boolean _allowEmpty = false,
                          boolean _onlyIfSuccessful = false, boolean _fingerprint = false,
                          boolean _defaultExcludes = true) {
        job.with {
            publishers {
                archiveArtifacts {
                    for (String p : patterns) {
                        pattern(p)
                    }
                    allowEmpty(_allowEmpty)
                    onlyIfSuccessful(_onlyIfSuccessful)
                    fingerprint(_fingerprint)
                    defaultExcludes(_defaultExcludes)
                }
            }
        }
        return this
    }

    /**
     * Searches for keywords in files or the console log and uses that to downgrade a build to be unstable or a failure.
     * @param regularExpression
     * @param fileSet
     * @param alsoCheckConsoleOutput
     * @param succeedIfFound
     * @param unstableIfFound
     * @return
     */
    T jenkinsTextFinder(String regularExpression = '^.*Reporting end of session with status 2.*$',
                                              String fileSet = '',
                                              boolean alsoCheckConsoleOutput = true,
                                              boolean succeedIfFound = false,
                                              Object unstableIfFound = true) {
        job.with {
            publishers {
                textFinder(regularExpression, fileSet, alsoCheckConsoleOutput, succeedIfFound, unstableIfFound)
            }
        }
        return this
    }

    /**
     * Automatically sets a description for the build after it has completed.
     * @param regularExpression
     * @param description
     * @param regularExpressionForFailed
     * @param descriptionForFailed
     * @param multiConfigurationBuild
     * @return
     */
    T setBuildDescription(String regularExpression = '^.*JENKINS_DESCRIPTION(.*)',
                                                String description = '',
                                                String regularExpressionForFailed = '^.*JENKINS_DESCRIPTION(.*)',
                                                String descriptionForFailed = '',
                                                boolean multiConfigurationBuild = false) {
        job.with {
            publishers {
                buildDescription(regularExpression, description, regularExpressionForFailed, descriptionForFailed, multiConfigurationBuild)
            }
        }
        return this
    }

    /**
     *
     * @param email
     * @return
     */
    T editableEmailNotification(Email email) {
        job.with {
            configure { project ->
                project / publishers << 'hudson.plugins.emailext.ExtendedEmailPublisher' {
                    recipientList email.getRecipient()
                    configuredTriggers {
                        if (email.getFailureTriggerSubject() != null) {
                            'hudson.plugins.emailext.plugins.trigger.FailureTrigger' emailTrigger(email.getFailureTriggerSubject())
                        }

                        if (email.getFixedTriggerSubject() != null) {
                            'hudson.plugins.emailext.plugins.trigger.FixedTrigger' emailTrigger(email.getFixedTriggerSubject())
                        }

                        if (email.getUnstableTriggerSubject() != null) {
                            'hudson.plugins.emailext.plugins.trigger.UnstableTrigger' emailTrigger(email.getUnstableTriggerSubject())
                        }

                        if (email.getAbortedTriggerSubject() != null) {
                            'hudson.plugins.emailext.plugins.trigger.AbortedTrigger' emailTrigger(email.getAbortedTriggerSubject())
                        }

                        if (email.getAlwaysTriggerSubject() != null) {
                            'hudson.plugins.emailext.plugins.trigger.AlwaysTrigger' emailTrigger(email.getAlwaysTriggerSubject())
                        }
                    }
                    contentType 'default'
                    defaultSubject email.getSubject()
                    defaultContent email.getContent()
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
        return this
    }

    protected Closure emailTrigger(String subjectValue) {
        return {
            email {
                recipientList ''
                subject subjectValue
                body '$PROJECT_DEFAULT_CONTENT'
                recipientProviders {
                    'hudson.plugins.emailext.plugins.recipients.ListRecipientProvider' {}
                    'hudson.plugins.emailext.plugins.recipients.RequesterRecipientProvider' {}
                }
                'attachmentsPattern' {}
                'attachBuildLog' 'false'
                'compressBuildLog' 'false'
                'replyTo' '$PROJECT_DEFAULT_REPLYTO'
                'contentType' 'project'
            }
        }
    }

    /**
     * Publishes JUnit test result reports.
     * @param The path to JUnit XML files in the Ant glob syntax.
     * @param _retainLongStdout If set, retains any standard output or error from a test suite in the test results after the build completes.
     * @return
     */
    T publishJUnitTestResultReport(String testReportXMLs, boolean _retainLongStdout = false) {
        job.with {
            publishers {
                archiveJunit(testReportXMLs) {
                    retainLongStdout(_retainLongStdout)
                }
            }
        }
        return this
    }

    /**
     * Execute a set of scripts at the end of the build.
     */
    T executeASetOfScripts(boolean _onlyIfBuildSucceeds = false) {
        job.with {
            publishers {
                postBuildScripts {
                    onlyIfBuildSucceeds(_onlyIfBuildSucceeds)
                    steps {
                        environmentVariables {
                            propertiesFile('env.properties')
                        }
                    }
                }
            }
        }
        return this
    }

    T executeShellScript(String shellScript, boolean _onlyIfBuildSucceeds = false) {
        job.with {
            publishers {
                postBuildScripts {
                    onlyIfBuildSucceeds(_onlyIfBuildSucceeds)
                    steps {
                        shell(shellScript)
                    }
                }
            }
        }
        return this
    }

    T executeGroovyScript(String groovyScipt, boolean _onlyIfBuildSucceeds = false) {
        job.with {
            publishers {
                postBuildScripts {
                    onlyIfBuildSucceeds(_onlyIfBuildSucceeds)
                    steps {
                        systemGroovyCommand(groovyScipt)
                    }
                }
            }
        }
        return this
    }

    /**
     * Executes Groovy scripts after a build.
     * @param groovyPostBuildScript Script String
     * @param behavior              Possible values for behavior:
                                         Behavior.DoNothing
                                         Behavior.MarkUnstable
                                         Behavior.MarkFailed
     * @return
     */
    T groovyPostBuild(String groovyPostBuildScript, Behavior behavior = Behavior.DoNothing) {
        job.with {
            publishers {
                groovyPostBuild(groovyPostBuildScript, behavior)
            }
        }
        return this
    }
}
