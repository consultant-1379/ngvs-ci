package com.ericsson.bss.job.washingmachine

class InvoicingWashingMachineRpmJobBuilder extends InvoicingWashingMachineJobBuilder {

    protected static final int TIMEOUT = 60

    @Override
    protected String getProjectName() {
        return super.getProjectName() + RPM_SUFFIX
    }

    @Override
    protected String getProjectDescription() {
        return 'Installs latest Invoicing RPM continuously to verify that everything works.'
    }

    @Override
    protected boolean hasEmailNotification() {
        return false
    }

    @Override
    protected int getWashingMachineTimeout() {
        return TIMEOUT
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
                            predefinedProp('BLAME_CONFIG_FILE', '/proj/eta-automation/tapas/config/invoicing/config/blame/general-blame-config.json,' +
                                '/proj/eta-automation/blame_mail/wm-rpm-blame-config-invoicing.json')
                            predefinedProp('STATUS', '$WM_BLAME_STATUS')
                            predefinedProp('DEFAULT_RECIPIENTS', 'invoicing_washingmachine_rpm@mailman.lmera.ericsson.se')
                        }
                    }
                }
            }
        }
    }
}
