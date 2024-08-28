package com.ericsson.bss.job.washingmachine

class CusWashingMachineJobBuilder extends AbstractWashingMachineJobBuilder {

    void prepareMailConfig() {
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_RECIPIENT, 'cus_washingmachine@mailman.lmera.ericsson.se')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_SUBJECT, 'Cus Washingmachine')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_CONTENT, 'Tapas session: $tapas_web_url')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_FAILURE_TRIGGER_SUBJECT, 'Cus Washingmachine Failed!')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_FIXED_TRIGGER_SUBJECT, 'Cus Washingmachine is back to normal!')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_UNSTABLE_TRIGGER_SUBJECT, 'Cus Washingmachine Jive tests failed!')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_ABORTED_TRIGGER_SUBJECT, 'Cus Washingmachine aborted or timeout!')
    }
}
