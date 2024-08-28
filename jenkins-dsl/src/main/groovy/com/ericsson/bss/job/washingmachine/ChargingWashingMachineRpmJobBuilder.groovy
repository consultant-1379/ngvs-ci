package com.ericsson.bss.job.washingmachine

class ChargingWashingMachineRpmJobBuilder extends ChargingWashingMachineJobBuilder {

    protected static final int TIMEOUT = 60

    @Override
    protected String getProjectName() {
        return super.getProjectName() + RPM_SUFFIX
    }

    @Override
    protected String getProjectDescription() {
        return 'Installs latest Charging RPM continiosly to verify that everything works.'
    }

    @Override
    void addChargingBuildParametersConfig() {
        job.with {
            parameters {
                choiceParam('HOST_SET', ['alternate', '1'],
                        'alternate: Alternate between 1 and 2 automatically<br/>\n' +
                                '1: CORE=vma-cha0018, ACCESS=vma-cha0019, DLB=vma-cha0020, CIL=vma-cha0017, MSV=vma-cha0016<br />')
                stringParam('CORERPMVERSION', 'LATEST', 'Core server rpm version to use. Default is LATEST')
                stringParam('ACCESSRPMVERSION', 'LATEST', 'Access server rpm version to use. Default is LATEST')
                stringParam('SERVICERPMVERSION', 'LATEST', 'Servicelogics rpm version to use. Default is LATEST')
                stringParam('DLBRPMVERSION', 'LATEST', 'Dlb rpm version to use. Default is LATEST')
                stringParam('JIVEVERSION', 'LATEST', 'Jive Version to use in JiveTest. Default is LATEST')
            }
        }
    }

    @Override
    protected int getWashingMachineTimeout() {
        return TIMEOUT
    }

    @Override
    void addPreScriptBuildSteps() {
        // rpm has no before script steps
    }

    @Override
    void addArchiveArtifactsConfig() {
        job.with {
            publishers {
                archiveArtifacts {
                    pattern('chargingrpms/*.rpm')
                    pattern('jive/*/*.jar')
                    allowEmpty(false)
                    onlyIfSuccessful(false)
                    fingerprint(false)
                    defaultExcludes(true)
                }
            }
        }
    }

    @Override
    void addTriggerParameterizedBuildOnOtherProjectsConfig() {
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
                            predefinedProp('BLAME_CONFIG_FILE', '/proj/eta-automation/tapas/config/charging/config/blame/general-blame-config.json,' +
                                '/proj/eta-automation/blame_mail/wm-rpm-blame-config-charging.json')
                            predefinedProp('SESSION_TIMESTAMP', '$WM_BLAME_TIMESTAMP')
                            predefinedProp('STATUS', '$WM_BLAME_STATUS')
                            predefinedProp('DEFAULT_RECIPIENTS', 'charging_washingmachine_rpm@mailman.lmera.ericsson.se')
                        }
                    }
                }
            }
        }
    }
}
