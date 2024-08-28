package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.util.scriptbuilders.ProjectVersionsScriptBuilder

class CilWashingMachineJobBuilder extends AbstractWashingMachineJobBuilder {

    private static final String CIL_VERSIONS_URL = 'https://arm.epk.ericsson.se/artifactory/proj-cil-release-local/com/ericsson/bss/' +
            'cil/server/cil-server-dv/maven-metadata.xml'

    @Override
    protected void prepareMailConfig() {
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_RECIPIENT, 'cil_washingmachine@mailman.lmera.ericsson.se')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_SUBJECT, 'CIL Washingmachine')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_CONTENT, 'Tapas session: $tapas_web_url')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_FAILURE_TRIGGER_SUBJECT, 'CIL Washingmachine Failed!')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_FIXED_TRIGGER_SUBJECT, 'CIL Washingmachine is back to normal!')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_UNSTABLE_TRIGGER_SUBJECT, 'CIL Washingmachine Jive tests failed!')
        mailConfig.put(AbstractWashingMachineJobBuilder.EMAIL_ABORTED_TRIGGER_SUBJECT, 'CIL Washingmachine aborted or timeout!')
    }

    @Override
    protected void addSpecificConfig() {
        job.with {
            blockOn('cil_washingmachine_eftf') {
                blockLevel('GLOBAL')
                scanQueueFor('ALL')
            }

            parameters {
                stringParam('MSV', 'vmx-cil018', 'The MSV that performs the OVF deploy and also will store info about targethost in zookeeper.')
                activeChoiceParam('RELEASEVERSION') {
                    description('Version of latest RELEASE verions of CIL-server-dv to install.')
                    filterable(false)
                    choiceType('SINGLE_SELECT')
                    groovyScript {
                        script(ProjectVersionsScriptBuilder.newBuilder(dslFactory)
                                .withUrl(CIL_VERSIONS_URL)
                                .build()
                        )
                        fallbackScript('return ["Error evaluating Groovy script.", ""]')
                    }
                }
            }

            scm {
                git {
                    remote {
                        url('ssh://gerrit.epk.ericsson.se:29418/eftf/cil')
                    }
                    clean(true)
                    branch('*/master')
                }
            }
        }
    }
}
