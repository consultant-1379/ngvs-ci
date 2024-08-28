package com.ericsson.bss.job.washingmachine

class RmcaWashingMachineRpmJobBuilder extends RmcaWashingMachineJobBuilder {

    protected static final int TIMEOUT = 120

    @Override
    protected String getProjectName() {
        return super.getProjectName() + RPM_SUFFIX
    }

    @Override
    protected String getProjectDescription() {
        return 'Installs latest RMCA RPM continiosly to verify that everything works.'
    }

    @Override
    protected boolean hasEmailNotification() {
        return false
    }

    @Override
    protected void addBuildParametersConfig() {
        job.with {
            parameters {
                choiceParam('HOST_SET', ['alternate', '1'],
                                'alternate: Alternate between the available clusters automatically<br/>\n' +
                                '1: RMCA=vma-rmca0007, MSV=vma-rmca0008, CIL=vma-rmca0009<br/>\n' +
                                'InstallNode is always vmx-rmca150')
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
                            predefinedProp('BLAME_CONFIG_FILE', '/proj/eta-automation/tapas/config/rmca/config/blame/general-blame-config.json,' +
                                '/proj/eta-automation/blame_mail/wm-rpm-blame-config-rmca.json')
                            predefinedProp('STATUS', '$WM_BLAME_STATUS')
                            predefinedProp('DEFAULT_RECIPIENTS', 'rmca_washingmachine_rpm@mailman.lmera.ericsson.se')
                        }
                    }
                }
            }
        }
    }

    @Override
    protected int getWashingMachineTimeout() {
        return TIMEOUT
    }
}
