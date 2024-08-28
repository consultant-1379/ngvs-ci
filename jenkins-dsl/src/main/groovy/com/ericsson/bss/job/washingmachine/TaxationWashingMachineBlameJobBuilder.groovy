package com.ericsson.bss.job.washingmachine

class TaxationWashingMachineBlameJobBuilder extends AbstractWashingMachineBlameJobBuilder {

    @Override
    protected void prepareParameters() {
        job.with {
            parameters {
                booleanParam('SEND_MAIL', true, 'If set, blame emails will be sent')
                stringParam('JENKINS_URL', '', 'The URL to the WM Jenkins session')
                stringParam('TAPAS_URL', '', 'The URL to the WM Tapas session')
                stringParam('JIVE_URL', '', 'The URL to the WM Jive session')
                stringParam('DEFAULT_RECIPIENTS', 'taxation_washingmachine@mailman.lmera.ericsson.se',
                        'Comma separated list of e-mail addresses that will always get the blame mail')
                stringParam('BLAME_CONFIG_FILE', '/proj/eta-automation/tapas/config/taxation/config/blame/general-blame-config.json,' +
                    '/proj/eta-automation/blame_mail/wm-blame-config-taxation.json', '')
                stringParam('UPSTREAM_JOB', '', 'The job that triggered this job')
                stringParam('SESSION_TIMESTAMP', '', 'Time when the WM session started')
                stringParam('STATUS', '', 'Status of the WM execution: success, error, testfailure or backtonormal')
                stringParam('CISCAT_RESULT', '', 'The result of the CIS-CAT tests')
            }
        }
    }

    @Override
    protected String getIncludedPatterns() {
        return 'taxationrpms/*.rpm, jive/*/*.jar'
    }
}