package com.ericsson.bss.job.washingmachine

class MsgWashingMachineJobBuilder extends AbstractWashingMachineJobBuilder {
    protected static final PROJECT_TO_BUILD_AFTER_BUILD = 'msg_washingmachine_blame'

    @Override
    protected boolean hasEmailNotification() {
        return false
    }

    @Override
    protected void addSpecificConfig() {
        addPermissionToCopyArtifact(PROJECT_TO_BUILD_AFTER_BUILD)
    }

    @Override
    protected void addPostBuildScripts() {
        job.with {
            publishers {
                postBuildScripts {
                    steps {
                        systemGroovyCommand(dslFactory.readFileFromWorkspace('scripts/washingmachine/wm_blame_status.groovy'))
                        environmentVariables {
                            propertiesFile('env.properties')
                        }
                    }
                    onlyIfBuildSucceeds(false)
                }
            }
        }
    }

    @Override
    protected void addArchiveArtifactsConfig() {
        job.with {
            publishers {
                archiveArtifacts {
                    pattern('msgrpms/*.rpm')
                    allowEmpty(true)
                    onlyIfSuccessful(false)
                    fingerprint(false)
                    defaultExcludes(true)
                }
            }
        }
    }

    @Override
    protected void addTriggerParameterizedBuildOnOtherProjectsConfig() {
        job.with {
            publishers {
                downstreamParameterized {
                    trigger(PROJECT_TO_BUILD_AFTER_BUILD) {
                        condition('FAILED_OR_BETTER')
                        parameters {
                            predefinedProp('JENKINS_URL', '$BUILD_URL')
                            predefinedProp('TAPAS_URL', '$tapas_web_url')
                            predefinedProp('UPSTREAM_JOB', '$JOB_NAME')
                            predefinedProp('BLAME_CONFIG_FILE', '/proj/eta-automation/tapas/config/msg/config/blame/general-blame-config.json,' +
                                '/proj/eta-automation/blame_mail/wm-blame-config-msg.json')
                            predefinedProp('STATUS', '$WM_BLAME_STATUS')
                            predefinedProp('SESSION_TIMESTAMP', '$WM_BLAME_TIMESTAMP')
                            predefinedProp('CISCAT_RESULT', '$CISCAT_RESULT')
                            predefinedProp('DEFAULT_RECIPIENTS', 'msg_washingmachine@mailman.lmera.ericsson.se')
                        }
                    }
                }
            }
        }
    }

}
