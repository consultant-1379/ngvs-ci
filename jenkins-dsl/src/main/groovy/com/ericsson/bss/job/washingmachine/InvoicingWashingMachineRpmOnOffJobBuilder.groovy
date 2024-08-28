package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.job.washingmachine.utils.WashingMachineConstantsHelper

@Deprecated
class InvoicingWashingMachineRpmOnOffJobBuilder extends AbstractWashingMachineOnOffJobBuilder {

    @Override
    protected String getProjectDescription() {
        return 'A job to enable and disable Invoicing WashingMachine RPM to allow troubleshooting on targethosts.'
    }

    @Override
    protected String getProjectToBuildName() {
        return projectName + WashingMachineConstantsHelper.WASHINGMACHINE_SUFFIX + WashingMachineConstantsHelper.RPM_SUFFIX
    }

    @Override
    protected void prepareMailConfig() {
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_RECIPIENT, 'invoicing_washingmachine_rpm@mailman.lmera.ericsson.se')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_SUBJECT, 'Invoicing WashingMachine RPM state changed to $ACTION')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_CONTENT,
                'Invoicing WashingMachine RPM state changed to $ACTION by $BUILD_USER_ID with reason \'$REASON\'')
    }
}
