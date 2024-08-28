package com.ericsson.bss.job.washingmachine

class EdmWashingMachineJobBuilder extends AbstractWashingMachineJobBuilder {

    void prepareMailConfig() {
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_RECIPIENT, 'edm_washingmachine@mailman.lmera.ericsson.se')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_SUBJECT, 'EDM Washingmachine')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_CONTENT, 'Tapas session: $tapas_web_url')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_FAILURE_TRIGGER_SUBJECT, 'EDM Washingmachine Failed!')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_FIXED_TRIGGER_SUBJECT, 'EDM Washingmachine is back to normal!')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_UNSTABLE_TRIGGER_SUBJECT, 'EDM Washingmachine Jive tests failed!')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_ABORTED_TRIGGER_SUBJECT, 'EDM Washingmachine aborted or timeout!')
    }

    @Override
    protected void addSpecificConfig() {
        addEdmBuildParametersConfig()
    }

    protected void addEdmBuildParametersConfig() {
        job.with {
            parameters {
                stringParam('PROCESSOROVFVERSION', 'LATEST', 'Version of Processor OVF to install. Use LATEST to build the latest working SNAPSHOT OVF.')
                stringParam('EXPOSUREOVFVERSION', 'LATEST', 'Version of Exposure OVF to install. Use LATEST to build the latest working SNAPSHOT OVF.')
            }
        }
    }
}
