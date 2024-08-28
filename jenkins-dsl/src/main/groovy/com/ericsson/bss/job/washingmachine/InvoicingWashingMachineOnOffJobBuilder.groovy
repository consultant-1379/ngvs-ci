package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.job.washingmachine.utils.WashingMachineConstantsHelper

@Deprecated
class InvoicingWashingMachineOnOffJobBuilder extends AbstractWashingMachineOnOffJobBuilder {

    @Override
    protected String getProjectDescription() {
        return 'A job to enable and disable Invoicing WashingMachine to allow troubleshooting on targethosts.'
    }

    @Override
    protected String getProjectToBuildName() {
        return projectName + WashingMachineConstantsHelper.WASHINGMACHINE_SUFFIX
    }

    @Override
    protected void prepareMailConfig() {
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_RECIPIENT, 'invoicing_washingmachine@mailman.lmera.ericsson.se')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_SUBJECT, 'Invoicing WashingMachine state changed to $ACTION')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_CONTENT,
                'Invoicing WashingMachine state changed to $ACTION by $BUILD_USER_ID with reason \'$REASON\'')
    }
}
