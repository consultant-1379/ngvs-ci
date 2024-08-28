package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.util.scriptbuilders.ProjectVersionsScriptBuilder

import javax.management.BadBinaryOpValueExpException

class ChargingWashingMachineJobBuilder extends AbstractWashingMachineJobBuilder {

    protected static final PROJECT_TO_BUILD_AFTER_BUILD = 'charging_washingmachine_blame'

    private static final COREOVFVERSION_URL = "https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/ericsson/bss/CHACORE/maven-metadata.xml"
    private static final ACCESSOVFVERSION_URL = "https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/" +
            "ericsson/bss/CHAACCESS/maven-metadata.xml"
    private static final DLBOVFVERSION_URL = "https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/ericsson/bss/CHADLB/maven-metadata.xml"

    @Override
    protected boolean hasEmailNotification() {
        return false
    }

    @Override
    protected void addSpecificConfig() {
        addChargingBuildParametersConfig()
        addPermissionToCopyArtifact(PROJECT_TO_BUILD_AFTER_BUILD)
    }

    @Override
    protected void addPostBuildScripts() {
        job.with {
            publishers {
                postBuildScripts {
                    steps {
                        systemGroovyCommand(dslFactory.readFileFromWorkspace('scripts/washingmachine/wm_blame_status.groovy'))
                        environmentVariables {
                            propertiesFile('env.properties')
                        }
                    }
                    onlyIfBuildSucceeds(false)
                }
            }
        }
    }

    protected void addChargingBuildParametersConfig() {
        job.with {
            parameters {
                choiceParam('HOST_SET', ['alternate', '1', '2'],
                        'alternate: Alternate between 1 and 2 automatically<br/>\n' +
                                '1: CORE=vma-cha0008, ACCESS=vma-cha0009, DLB=vma-cha0010, CIL=vma-cha0007, MSV=vma-cha0006<br/>\n' +
                                '2: CORE=vma-cha0013, ACCESS=vma-cha0014, DLB=vma-cha0015, CIL=vma-cha0012, MSV=vma-cha0011')
                stringParam('CORESTAGINGVERSION', 'LATEST', '')
                stringParam('ACCESSSTAGINGVERSION', 'LATEST', '')
                stringParam('DLBSTAGINGVERSION', 'LATEST', '')
                activeChoiceParam('COREOVFVERSION') {
                    description('Version of Core OVF to install. Use LATEST to build the latest working SNAPSHOT OVF.')
                    filterable(false)
                    choiceType('SINGLE_SELECT')
                    groovyScript {
                        script(ProjectVersionsScriptBuilder.newBuilder(dslFactory)
                                .withUrl(COREOVFVERSION_URL)
                                .withLatestVersion()
                                .build()
                        )
                        fallbackScript('return ["Error evaluating Groovy script.", ""]')
                    }
                }
                activeChoiceParam('ACCESSOVFVERSION') {
                    description('Version of Access OVF to install. Use LATEST to build the latest working SNAPSHOT OVF.')
                    filterable(false)
                    choiceType('SINGLE_SELECT')
                    groovyScript {
                        script(ProjectVersionsScriptBuilder.newBuilder(dslFactory)
                                .withUrl(ACCESSOVFVERSION_URL)
                                .withLatestVersion()
                                .build()
                        )
                        fallbackScript('return ["Error evaluating Groovy script.", ""]')
                    }
                }
                activeChoiceParam('DLBOVFVERSION') {
                    description('Version of Dlb OVF to install. Use LATEST to build the latest working SNAPSHOT OVF.')
                    filterable(false)
                    choiceType('SINGLE_SELECT')
                    groovyScript {
                        script(ProjectVersionsScriptBuilder.newBuilder(dslFactory)
                                .withUrl(DLBOVFVERSION_URL)
                                .withLatestVersion()
                                .build()
                        )
                        fallbackScript('return ["Error evaluating Groovy script.", ""]')
                    }
                }
                stringParam('JIVEVERSION', 'LATEST', 'Version of JIVE to use, LATEST is default')
            }
        }
    }

    @Override
    protected void addArchiveArtifactsConfig() {
        job.with {
            publishers {
                archiveArtifacts {
                    pattern('com.ericsson.bss.rm.charging.integration/rpm/target/rpm/*/RPMS/noarch/*.rpm')
                    pattern('com.ericsson.bss.rm.charging.integration/build/target/*.zip')
                    pattern('com.ericsson.bss.rm.charging.integration/rpm/target/com.ericsson.bss-content.txt')
                    pattern('com.ericsson.bss.rm.charging.integration/rpm/target/rpm-content.txt, chargingrpms/*.rpm')
                    pattern('jive/*/*.jar')
                    allowEmpty(false)
                    onlyIfSuccessful(false)
                    fingerprint(false)
                    defaultExcludes(true)
                }
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
                            predefinedProp('BLAME_CONFIG_FILE', '/proj/eta-automation/tapas/config/charging/config/blame/general-blame-config.json,' +
                                '/proj/eta-automation/blame_mail/wm-blame-config-charging.json')
                            predefinedProp('SESSION_TIMESTAMP', '$WM_BLAME_TIMESTAMP')
                            predefinedProp('STATUS', '$WM_BLAME_STATUS')
                            predefinedProp('CISCAT_RESULT', '$CISCAT_RESULT')
                            predefinedProp('DEFAULT_RECIPIENTS', 'charging_washingmachine@mailman.lmera.ericsson.se')
                        }
                    }
                }
            }
        }
    }
}
