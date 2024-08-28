package com.ericsson.bss.job.washingmachine

class MaptWashingMachineJobBuilder extends AbstractWashingMachineJobBuilder {

    protected static final PROJECT_TO_BUILD_AFTER_BUILD = 'mapt_washingmachine_blame'

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
                    pattern('maptrpms/*.rpm')
                    pattern('*/jive/*.jar')
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
                            predefinedProp('JIVE_URL', '$jive_web_url')
                            predefinedProp('UPSTREAM_JOB', '$JOB_NAME')
                            predefinedProp('BLAME_CONFIG_FILE', '/proj/eta-automation/tapas/config/mapt/config/blame/general-blame-config.json,' +
                                    '/proj/eta-automation/blame_mail/wm-blame-config-mapt.json')
                            predefinedProp('SESSION_TIMESTAMP', '$WM_BLAME_TIMESTAMP')
                            predefinedProp('STATUS', '$WM_BLAME_STATUS')
                            predefinedProp('CISCAT_RESULT', '$CISCAT_RESULT')
                            predefinedProp('DEFAULT_RECIPIENTS', 'mapt_washingmachine@mailman.lmera.ericsson.se')
                        }
                    }
                }
            }
        }
    }
}
