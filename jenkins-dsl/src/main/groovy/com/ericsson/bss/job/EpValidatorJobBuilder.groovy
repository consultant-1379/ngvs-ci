package com.ericsson.bss.job

import com.ericsson.bss.AbstractTapasJobBuilder
import javaposse.jobdsl.dsl.Job

public class EpValidatorJobBuilder extends AbstractTapasJobBuilder {

    protected List<String> resourceProfiles = []
    protected Map predefinedProperties = [:]

    protected boolean useCil = true
    protected boolean useDvFile = false
    protected boolean useJiveTests = false
    protected boolean useSeleniumTests = false
    protected boolean useResourceProfile = true
    protected boolean useTwoTargethosts = false

    protected List booleanFalseTrueList = ['false', 'true']

    private String buildDescriptionString = "\$TARGETHOST"
    protected String extraDescription = ""
    protected String jiveMetaData = ""
    protected String targethostDescription = ""
    protected String targethostDescription2 = ""
    protected String seleniumMetaData = ""
    protected String versionLocation = ""
    protected String versionLocation2 = ""

    public Job build() {
        if (useTwoTargethosts) {
            buildDescriptionString += ",\$TARGETHOST2"
        }
        super.buildDescription = buildDescriptionString
        suiteFile = suite.split('/')[-1].replace('.xml', '_\${TARGETHOST}.xml')
        preparePredefinedProp()
        initProject(dslFactory.freeStyleJob(jobName))
        setDescription("<br>\n" +
                       "This job is used to install " + projectName + " on a team cluster, load an EP and run some jivetests.<br>\n" +
                       extraDescription)
        addTimeoutAndAbortConfig(timeoutForJob)
        deleteWorkspaceBeforeBuildStarts()

        Map env_list = getInjectVariables()
        env_list.remove("HOME")
        injectEnv(env_list)
        job.with {
            steps {
                environmentVariables {
                    propertiesFile('env.properties')
                }
            }
            publishers {
                wsCleanup()
            }
        }
        return job
    }

    @Override
    protected void setInputParameters() {
        addSelectClusterParameter('Choose cluster to install')
        addClusterReferenceParameter('MSV', 'The MSV for this cluster')
        if (useCil) {
            addClusterReferenceParameter('CIL', 'The CIL for this cluster')
            predefinedProperties << [CIL: '\$CIL']
        }
        addClusterReferenceParameter('TARGETHOST', targethostDescription)
        addVersionChoiceParam('VERSION', versionLocation,
                              'Base version of ' + projectName + ' to install. Use LATEST to get the latest working SNAPSHOT from Washingmachine.')
        if (useTwoTargethosts) {
            addClusterReferenceParameter('TARGETHOST2', targethostDescription2)
            addVersionChoiceParam('VERSION2', versionLocation2,
                                  'Version 2 of ' + projectName + ' OVF to install. Use LATEST to get the latest working SNAPSHOT OVF from Washingmachine.')
            predefinedProperties << [TARGETHOST2: '\$TARGETHOST2', VERSION2: '\$VERSION2']
        }
        job.with {
            parameters {
                choiceParam('INSTALL_TARGETHOST', booleanFalseTrueList,
                            'Install ' + projectName +' targethost to your cluster. This will rollback MSV and CIL. MSV and CIL needs to be prepared.')
                if (useDvFile) {
                    stringParam('DVFILE', '',
                                'Optional. A custom file to use instead of an artifact from ARM (' + projectName + '-*-dv.tar.gz). ' +
                                'The file must be accessible from Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL. ' +
                                'Leave empty to download the specified VERSION from ARM.')
                    predefinedProperties << [DVFILE: '\$DVFILE']
                    if (useTwoTargethosts) {
                        stringParam('DVFILE2', '',
                                    'Optional. A custom file to use instead of an artifact from ARM (' + projectName + '-*-dv.tar.gz). ' +
                                    'The file must be accessible from Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL. ' +
                                    'Leave empty to download the specified VERSION2 from ARM.')
                        predefinedProperties << [DVFILE2: '\$DVFILE2']
                    }
                }
                if (useResourceProfile) {
                    choiceParam('RESOURCE_PROFILE', resourceProfiles, 'Specifies how much hardware resources (CPU and RAM) ' +
                                'the targethosts should be deployed with. Normally the "TestSystem" profile should be used.')
                    predefinedProperties << [RESOURCE_PROFILE: '\$RESOURCE_PROFILE']
                }
                stringParam('EPFILE', '',
                            'A custom file to use instead of an artifact from ARM (*.tar.gz). The file must be accessible from Jenkins as a filepath' +
                            ' (ie /workarea/.. or /proj/..) or a URL. Leave empty to download the specified VERSION from ARM.')
                stringParam('EPDELTA', '', 'Delta version of the EP, e.g (EP04A)')
                if (useTwoTargethosts) {
                    stringParam('EPFILE2', '',
                                'A custom file to use be used on TARGETHOST2 instead of an artifact from ARM (*.tar.gz). The file must be accessible from ' +
                                'Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL. Leave empty to download the specified VERSION from ARM.')
                    stringParam('EPDELTA2', '', 'Delta version of the EP on TARGETHOST2, e.g (EP04A)')
                }
                stringParam('VMAPI_PREFIX', projectName + '.',
                            'To use correct credentials and vCenter. Normally the product the host belongs to. Ex: "cil.", "charging.", "cpm.".')
                stringParam('PRODUCT', projectName, 'The product the machine belongs to, normally the same as VMAPI_PREFIX (but WITHOUT the ending dot).' +
                                                    ' Ex: "rmca", "charging", "cil", "cpm", "coba", "ss7translator", "invoicing".')
                choiceParam('CREATE_SNAPSHOTS', booleanFalseTrueList,
                            'Use this if you want to create an snapshot of a base state, that you can revert to a clean state if your EP fails.\n' +
                            'NOTE: "DO_ROLLBACK need to be false"')
                choiceParam('DO_ROLLBACK', booleanFalseTrueList,
                            'Use this if you want to rollback to your state created with "CREATE_SNAPSHOT".\n' +
                            'This will fail if no snapshot has been created.\n' +
                            'NOTE: "CREATE_SNAPSHOTS need to be false"')
                if (useJiveTests) {
                    choiceParam('RUN_JIVE_TESTS', booleanFalseTrueList, 'Should Jive tests be executed after installation')
                    addVersionChoiceParam('AVAILABLE_JIVE_VERSIONS', jiveMetaData, 'The Jive version to use. ', true)
                    addReferenceParameter('JIVE_VERSION', 'AVAILABLE_JIVE_VERSIONS',
                                          'Select a Jive version from above or supply your own from a location Jenkins can access, ie: http:// or /proj/...')
                }
                if (useSeleniumTests) {
                    choiceParam('RUN_SELENIUM_TESTS', booleanFalseTrueList, 'Should Jive tests be executed after installation')
                    addVersionChoiceParam('AVAILABLE_SELENIUM_VERSIONS', seleniumMetaData, 'The Selenium version to use. ', true)
                    addReferenceParameter('SELENIUM_VERSION', 'AVAILABLE_SELENIUM_VERSIONS',
                                          'Select a Selenium version from above or supply your own file. The ' +
                                          'file must be accessible from Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL.')
                }
            }
        }
        setExtraOptions()
    }

    protected void setExtraOptions() {
        return
    }

    protected preparePredefinedProp() {
        predefinedProperties = [TARGETHOST: '\$TARGETHOST',
                MSV: '\$MSV',
                VERSION: '\$VERSION',
                VMAPI_PREFIX: '\$VMAPI_PREFIX',
                PRODUCT: '\$PRODUCT',
                DO_ROLLBACK: 'true']
    }

    @Override
    protected String getAdditionalTapasShell() {

        String additionalTapasString = 'HOST_CONFIG_DIR="/proj/eta-automation/tapas/config/${PRODUCT}/config/"\n'

        if (useJiveTests || useSeleniumTests) {
            additionalTapasString += '\n'+
                    'EXECUTION_HOST_PORT=$((${ALLOCATED_PORT}+1))\n'
        }
        return additionalTapasString
    }

    @Override
    protected String getTapasParameters() {
        String params = '--define=__TARGETHOST__=${TARGETHOST} \\\n' +
                        '--define=__VERSION__="${VERSION}" \\\n' +
                        '--define=__MSV__=${MSV} \\\n' +
                        '--define=__DVFILE__=\${DVFILE} \\\n' +
                        '--define=__EPFILE__=\${EPFILE} \\\n' +
                        '--define=__EPDELTA__=\${EPDELTA} \\\n' +
                        '--define=__VMAPI_PROFILE_PREFIX__=${VMAPI_PREFIX} \\\n' +
                        '--define=__DO_ROLLBACK__=${DO_ROLLBACK} \\\n' +
                        '--define=__CREATE_SNAPSHOTS__=${CREATE_SNAPSHOTS} \\\n'
        if (useTwoTargethosts) {
            params +=   '--define=__TARGETHOST2__=${TARGETHOST2} \\\n' +
                        '--define=__VERSION2__="${VERSION2}" \\\n' +
                        '--define=__DVFILE2__=\${DVFILE2} \\\n' +
                        '--define=__EPFILE2__=\${EPFILE2} \\\n' +
                        '--define=__EPDELTA2__=\${EPDELTA2} \\\n'
        }
        if (useCil) {
            params +=   '--define=__CIL__=\${CIL} \\\n'
        }

        if (useJiveTests) {
            params +=   '--define=__RUN_JIVE_TESTS__=\${RUN_JIVE_TESTS} \\\n' +
                        '--define=__JIVEVERSION__=\${JIVE_VERSION} \\\n'
        }
        if (useSeleniumTests) {
            params +=   '--define=__RUN_SELENIUM_TESTS__=\${RUN_SELENIUM_TESTS} \\\n' +
                        '--define=__SELENIUMVERSION__=\${SELENIUM_VERSION} \\\n'
        }
        if (useJiveTests || useSeleniumTests) {
            params +=   '--define=__EXECUTION_HOST_PORT__=\${EXECUTION_HOST_PORT} \\\n'
        }
        params +=       getOptionalTapasParameters()
        return params
    }

    protected String getOptionalTapasParameters() {
        return ""
    }

    @Override
    protected String getAdditionalFinishingTapasShell() {
        return  '\n' +
                'SUCCESS="SUCCESS"\n' +
                'echo "SUCCESS=\$SUCCESS" >> env.properties\n' +
                'echo \$SUCCESS\n'
    }

    @Override
    protected void setExtraShell() {
        job.with {
            steps {
                conditionalSteps {
                    condition {
                        stringsMatch('\${INSTALL_TARGETHOST}', 'true', false)
                    }
                    steps {
                        downstreamParameterized {
                            trigger(projectName + '_targethost_install') {
                                block {
                                    buildStepFailure('FAILURE')
                                    failure('FAILURE')
                                    unstable('UNSTABLE')
                                }
                                parameters {
                                    predefinedProps(predefinedProperties)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}