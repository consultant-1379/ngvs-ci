package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.job.washingmachine.utils.WashingMachineConstantsHelper

@Deprecated
class CilWashingMachineOnOffJobBuilder extends AbstractWashingMachineOnOffJobBuilder {

    @Override
    protected String getProjectDescription() {
        return 'A job to enable and disable CIL WashingMachine to allow troubleshooting on targethosts.'
    }

    @Override
    protected String getProjectToBuildName() {
        return projectName + WashingMachineConstantsHelper.WASHINGMACHINE_SUFFIX
    }

    @Override
    protected void prepareMailConfig() {
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_RECIPIENT, 'cil_washingmachine@mailman.lmera.ericsson.se')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_SUBJECT, 'CIL WashingMachine state changed to $ACTION')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_CONTENT, 'CIL WashingMachine state changed to $ACTION by $BUILD_USER_ID with reason \'$REASON\'')
    }
}
