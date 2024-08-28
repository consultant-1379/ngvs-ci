package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.job.washingmachine.utils.WashingMachineConstantsHelper

@Deprecated
class CobaWashingMachineOnOffJobBuilder extends AbstractWashingMachineOnOffJobBuilder {

    @Override
    protected String getProjectDescription() {
        return 'A job to enable and disable COBA WashingMachine to allow troubleshooting on targethosts.'
    }

    @Override
    protected String getProjectToBuildName() {
        return projectName + WashingMachineConstantsHelper.WASHINGMACHINE_SUFFIX
    }

    @Override
    protected void prepareMailConfig() {
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_RECIPIENT, 'coba_washingmachine@mailman.lmera.ericsson.se')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_SUBJECT, 'COBA WashingMachine state changed to $ACTION')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_CONTENT, 'COBA WashingMachine state changed to $ACTION by $BUILD_USER_ID with reason \'$REASON\'')
    }
}
