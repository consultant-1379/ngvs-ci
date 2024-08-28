package com.ericsson.bss.job

import com.ericsson.bss.AbstractJobBuilder
import javaposse.jobdsl.dsl.Job

public class IntegrationWashinmachineJobBuilder extends AbstractJobBuilder {

    protected Map predefinedProperties = [:]

    private String chaCoreVersionLocation = 'https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/ericsson/bss/rm/charging.core/;1.5.0'
    private String chaAccessVersionLocation = 'https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/ericsson/bss/rm/charging.access/;1.5.0'
    private String chaDlbVersionLocation = 'https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/ericsson/bss/rm/charging.dlb/;1.5.0'
    private String cpmVersionLocation = 'https://arm.epk.ericsson.se/artifactory/proj-cpm-release-local/com/ericsson/bss/rm/cpm/umi/cpm/'
    private String cobaVersionLocation = 'https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/COBA/,' +
            'https://arm.epk.ericsson.se/artifactory/simple/proj-coba-release-local/com/ericsson/bss/rm/coba/integration/cobapackage/;1.6.0'
    private String cpiVersionLocation = 'https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/CPI/,' +
            'https://arm.epk.ericsson.se/artifactory/proj-cpi-release/com/ericsson/bss/cpi/package/umi/cpi/;1.1.0'
    private String colVersionLocation = 'https://arm.epk.ericsson.se/artifactory/simple/proj-collection-release-local/' +
            'com/ericsson/bss/rm/collection/opd/collection/'
    private String cusVersionLocation = 'https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/CUS/'
    private String edmProcVersionLocation = 'https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/EDMEVPROC/'
    private String edmExpoVersionLocation = 'https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/EDMEVEXPO/'
    private String epsMasterVersionLocation = 'https://arm.epk.ericsson.se/artifactory/proj-eps-release-local/com/ericsson/bss/rm/eps/EPSMASTER/,' +
            'https://arm.epk.ericsson.se/artifactory/proj-bssf-release-local/com/ericsson/bss/EPSMASTER'
    private String epsWorkerVersionLocation = 'https://arm.epk.ericsson.se/artifactory/proj-eps-release-local/com/ericsson/bss/rm/eps/EPSWORKER/,' +
            'https://arm.epk.ericsson.se/artifactory/proj-bssf-release-local/com/ericsson/bss/EPSWORKER'
    private String ermsVersionLocation = 'https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/ERMS/,' +
            'https://arm.epk.ericsson.se/artifactory/proj-erms-release/com/ericsson/bss/rm/erms/opd/erms/;0.4.0'
    private String finVersionLocation = 'https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/FINANCE/'
    private String invControllerVersionLocation = 'https://arm.epk.ericsson.se/artifactory/simple/proj-invoicing-release-local/com/ericsson/bss/INVCONTROLLER'
    private String invProcessorVersionLocation = 'https://arm.epk.ericsson.se/artifactory/simple/proj-invoicing-release-local/com/ericsson/bss/INVPROCESSOR'
    private String maptVersionLocation = 'https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/MAPT/,'+
            'https://arm.epk.ericsson.se/artifactory/proj-maptranslator-release/com/ericsson/bss/rm/maptranslator/integration/maptpackage/;1.6.0'
    private String numVersionLocation = 'https://arm.epk.ericsson.se/artifactory/simple/proj-num-release-local/com/ericsson/bss/rm/num/integration/numpackage'
    private String rmcaVersionLocation = 'https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/RMCA/,' +
            'https://arm.epk.ericsson.se/artifactory/simple/proj-rmca-release-local/com/ericsson/bss/rmca/integration/rmcapackage/;24.2.0'

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))

        preparePredefinedProp()
        addJobParameters()
        addInitShell()
        setPropertiesFile('env.properties')
        addConditionalSteps()
        deleteWorkspaceAfterBuild()
        return job
    }

    private void addJobParameters() {
        addInitParameters()

        addParametersForTpgs('CHACORE', chaCoreVersionLocation, 'CHA Core')

        addParametersForTpgs('CHAACCESS', chaAccessVersionLocation, 'CHA Access')
        addStringParam('FILEFETCHERHOST', '', '<b>optional</b> filefetcherhost can be specified. Default is Access targethost. '+
                'MM-node where CDRs should be fetched from, related to Charging installation <br><br><br>')

        addParametersForTpgs('CHADLB', chaDlbVersionLocation, 'CHA DLB')

        addParametersForTpgs('CPM', cpmVersionLocation, 'CPM')

        addParametersForTpgs('COBA', cobaVersionLocation, 'COBA')

        addParametersForTpgs('CPI', cpiVersionLocation, 'CPI')

        addParametersForTpgs('COL', colVersionLocation, 'COL')

        addParametersForTpgs('CUS', cusVersionLocation, 'CUS')

        addParametersForTpgs('EDMPROC', edmProcVersionLocation, 'EDM Proc')

        addParametersForTpgs('EDMEXPO', edmExpoVersionLocation, 'EDM Expo')

        addParametersForTpgs('EPSMASTER', epsMasterVersionLocation, 'EPS Master')

        addParametersForTpgs('EPSWORKER', epsWorkerVersionLocation, 'EPS Worker')

        addParametersForTpgs('ERMS', ermsVersionLocation, 'ERMS')

        addParametersForTpgs('FIN', finVersionLocation, 'FIN')

        addParametersForTpgs('INVCONTROLLER', invControllerVersionLocation, 'INV Controller')

        addParametersForTpgs('INVPROCESSOR', invProcessorVersionLocation, 'INV Processor')

        addParametersForTpgs('MAPT', maptVersionLocation, 'MAPT')

        addParametersForTpgs('NUM', numVersionLocation, 'NUM')

        addParametersForTpgs('RMCA', rmcaVersionLocation, 'RMCA')
    }

    protected void addConditionalSteps() {
        addConditionalStepWithRegExpression('\${INSTALLCOBA}', 'coba', preparePredefinedProp() << [TARGETHOST: '\$COBA_TARGETHOST',
                                                                                                   VERSION: '\$COBA_VERSION'])

        addConditionalStepWithRegExpression('\${INSTALLCPM}', 'cpm', preparePredefinedProp() << [TARGETHOST: '\$CPM_TARGETHOST',
                                                                                                 VERSION: '\$CPM_VERSION',
                                                                                                 RESET_CIL: 'false'])

        addConditionalStepWithRegExpressionOnRemote('\${INSTALLRMCA}', 'rmca', 'eforge', preparePredefinedProp() << [VERSION: '\$RMCA_VERSION',
                                                                                                                     TARGETHOST: '\$RMCA_TARGETHOST',
                                                                                                                     RUN_SIMULATORS: 'false'])

        addConditionalStepWithRegExpression('\${INSTALLNUM}', 'num', preparePredefinedProp() << [TARGETHOST: '\$NUM_TARGETHOST',
                                                                                                 VERSION: '\$NUM_VERSION',
                                                                                                 INSTALL_TESTDATA: 'false'])

        addConditionalStepWithRegExpression('\${INSTALLCHA}', 'charging', preparePredefinedProp() << [INSTALLTYPE: 'full+dlb',
                                                                                                      CHARGINGCORE: '\$CHACORE_TARGETHOST',
                                                                                                      COREOVFVERSION: '\$CHACORE_VERSION',
                                                                                                      CHARGINGACCESS: '\$CHAACCESS_TARGETHOST',
                                                                                                      ACCESSOVFVERSION: '\$CHAACCESS_VERSION',
                                                                                                      CHARGINGDLB: '\$CHADLB_TARGETHOST',
                                                                                                      DLBOVFVERSION: '\$CHADLB_VERSION',
                                                                                                      DISABLE_CACHE: 'false',
                                                                                                      INSTALL_TEST_UTILS: 'false'])

        addConditionalStepWithRegExpression('\${INSTALLMAPT}', 'mapt', preparePredefinedProp() << [TARGETHOST: '\$MAPT_TARGETHOST',
                                                                                                   VERSION: '\$MAPT_VERSION',
                                                                                                   INSTALL_TESTDATA: 'false'])

        addConditionalStepWithRegExpression('\${INSTALLEPS}', 'eps', preparePredefinedProp() << [INSTALLTYPE: 'full',
                                                                                                 TARGETHOST: '\$EPSMASTER_TARGETHOST',
                                                                                                 VERSION: '\$EPSMASTER_VERSION',
                                                                                                 TARGETHOST2: '\$EPSWORKER_TARGETHOST',
                                                                                                 VERSION2: '\$EPSWORKER_TARGETHOST'])

        addConditionalStepWithRegExpression('\${INSTALLEDM}', 'edm', preparePredefinedProp() << [INSTALLTYPE: 'full',
                                                                                                 TARGETHOST: '\$EDMPROC_TARGETHOST',
                                                                                                 VERSION: '\$EDMPROC_VERSION',
                                                                                                 TARGETHOST2: '\$EDMEXPO_TARGETHOST',
                                                                                                 VERSION2: '\$EDMEXPO_TARGETHOST'])

        addConditionalStepWithRegExpression('\${INSTALLCPI}', 'cpi', preparePredefinedProp() << [TARGETHOST: '\$CPI_TARGETHOST',
                                                                                                 VERSION: '\$CPI_VERSION',
                                                                                                 INSTALL_TESTDATA: 'false'])

        addConditionalStepWithRegExpression('\${INSTALLCOL}', 'collection', preparePredefinedProp() << [TARGETHOST: '\$COL_TARGETHOST',
                                                                                                        VERSION: '\$COL_VERSION',
                                                                                                        INSTALL_TESTDATA: 'false'])

        addConditionalStepWithRegExpression('\${INSTALLCUS}', 'cus', preparePredefinedProp() << [TARGETHOST: '\$CUS_TARGETHOST',
                                                                                                      VERSION: '\$CUS_VERSION'])

        addConditionalStepWithRegExpression('\${INSTALLERMS}', 'erms', preparePredefinedProp() << [TARGETHOST: '\$ERMS_TARGETHOST',
                                                                                                   VERSION: '\$ERMS_VERSION'])

        addConditionalStepWithRegExpression('\${INSTALLFIN}', 'finance', preparePredefinedProp() << [TARGETHOST: '\$FIN_TARGETHOST',
                                                                                                     VERSION: '\$FIN_VERSION',
                                                                                                     INSTALL_TESTDATA: 'false'])

        addConditionalStepWithRegExpression('\${INSTALLINV}', 'invoicing', preparePredefinedProp() << [INSTALLTYPE: 'full',
                                                                                                       TARGETHOST: '\$INVCONTROLLER_TARGETHOST',
                                                                                                       VERSION: '\$INVCONTROLLER_VERSION',
                                                                                                       TARGETHOST2: '\$INVPROCESSOR_TARGETHOST',
                                                                                                       VERSION2: '\$INVPROCESSOR_VERSION'])
    }

    private void addInitParameters() {
        addListChoiceParam('RUN_POST_INSTALL_CONFIG', [false, true], 'Run post deploy configure, i.e adding testdata and configuration')
        addStringParam('VMAPI_PREFIX', '',
                'To use correct credentitials and vCenter. Normally the product the host belongs to. Ex: "cil.", "charging.", "cpm.".')
        addStringParam('PRODUCT', '',
                'The product the TARGETHOST belongs to. Normally the same as VMAPI_PREFIX (but WITHOUT the ending dot). Ex: "charging", "cil", "cpm".')

        addStringParam('MSV_TARGETHOST', '', 'The machine with MSV installed on it')
        addStringParam('CIL_TARGETHOST', '', 'The machine with CIL installed on it')
        addStringParam('MSG_TARGETHOST', '',
                'MSG host to use during install. <b>Leave empty if you don\'t to use any specific MSG and instead use a MSGbroker</b> <br><br><br>')
    }

    private void addParametersForTpgs(String tpgName, String versionLocation, String tpgDescription) {
        addStringParam(tpgName + '_TARGETHOST', '', getTargetHostDescription(tpgDescription))
        addVersionChoiceParam(tpgName + '_VERSION', versionLocation, getVersionDescription(tpgDescription))
    }

    private void addInitShell() {
        job.with {
            steps {
                conditionalSteps {
                    condition {
                        alwaysRun()
                    }
                    runner('Fail')
                    steps {
                        shell(getInitShell())
                    }
                }
            }
        }
    }

    protected preparePredefinedProp() {
        predefinedProperties = [CIL: '\$CIL_TARGETHOST',
                                MSV: '\$MSV_TARGETHOST',
                                RESOURCE_PROFILE: 'TestSystem',
                                VMAPI_PREFIX: '\$VMAPI_PREFIX',
                                PRODUCT: '\$PRODUCT',
                                DO_ROLLBACK: 'false',
                                RUN_POST_INSTALL_CONFIG: '\$RUN_POST_INSTALL_CONFIG',
                                MSG: '\$MSG_TARGETHOST']
    }

    private String getInitShell() {
        return 'INSTALLCHA=false\n' +
                'if [ -n "${CHACORE_TARGETHOST}" -a -n "${CHAACCESS_TARGETHOST}" -a -n "${CHADLB_TARGETHOST}" ]; then\n' +
                '  INSTALLCHA=true\n' +
                'fi\n' +
                'echo "INSTALLCHA=$INSTALLCHA" >> env.properties\n\n' +
                'INSTALLCPM=false\n' +
                'if [ -n "${CPM_TARGETHOST}" ]; then\n' +
                '  INSTALLCPM=true\n' +
                'fi\n' +
                'echo "INSTALLCPM=$INSTALLCPM" >> env.properties\n\n' +
                'INSTALLCOBA=false\n' +
                'if [ -n "${COBA_TARGETHOST}" ]; then\n' +
                '  INSTALLCOBA=true\n' +
                'fi\n' +
                'echo "INSTALLCOBA=$INSTALLCOBA" >> env.properties\n\n' +
                'INSTALLCPI=false\n' +
                'if [ -n "${CPI_TARGETHOST}" ]; then\n' +
                '  INSTALLCPI=true\n' +
                'fi\n' +
                'echo "INSTALLCPI=$INSTALLCPI" >> env.properties\n\n' +
                'INSTALLCOL=false\n' +
                'if [ -n "${COL_TARGETHOST}" ]; then\n' +
                '  INSTALLCOL=true\n' +
                'fi\n' +
                'echo "INSTALLCOL=$INSTALLCOL" >> env.properties\n\n' +
                'INSTALLCUS=false\n' +
                'if [ -n "${CUS_TARGETHOST}" ]; then\n' +
                '  INSTALLCUS=true\n' +
                'fi\n' +
                'echo "INSTALLCUS=$INSTALLCUS" >> env.properties\n\n' +
                'INSTALLEDM=false\n' +
                'if [ -n "${EDMPROC_TARGETHOST}" -a -n "${EDMEXPO_TARGETHOST}" ]; then\n' +
                '  INSTALLEDM=true\n' +
                'fi\n' +
                'echo "INSTALLEDM=$INSTALLEDM" >> env.properties\n\n' +
                'INSTALLEPS=false\n' +
                'if [ -n "${EPSMASTER_TARGETHOST}" -a -n "${EPSWORKER_TARGETHOST}" ]; then\n' +
                '  INSTALLEPS=true\n' +
                'fi\n' +
                'echo "INSTALLEPS=$INSTALLEPS" >> env.properties\n\n' +
                'INSTALLERMS=false\n' +
                'if [ -n "${ERMS_TARGETHOST}" ]; then\n' +
                '  INSTALLERMS=true\n' +
                'fi\n' +
                'echo "INSTALLERMS=$INSTALLERMS" >> env.properties\n\n' +
                'INSTALLFIN=false\n' +
                'if [ -n "${FIN_TARGETHOST}" ]; then\n' +
                '  INSTALLFIN=true\n' +
                'fi\n' +
                'echo "INSTALLFIN=$INSTALLFIN" >> env.properties\n\n' +
                'INSTALLINV=false\n' +
                'if [ -n "${INVCONTROLLER_TARGETHOST}" -a -n "${INVPROCESSOR_TARGETHOST}" ]; then\n' +
                '  INSTALLINV=true\n' +
                'fi\n' +
                'echo "INSTALLINV=$INSTALLINV" >> env.properties\n\n' +
                'INSTALLMAPT=false\n' +
                'if [ -n "${MAPT_TARGETHOST}" ]; then\n' +
                '  INSTALLMAPT=true\n' +
                'fi\n' +
                'echo "INSTALLMAPT=$INSTALLMAPT" >> env.properties\n\n' +
                'INSTALLNUM=false\n' +
                'if [ -n "${NUM_TARGETHOST}" ]; then\n' +
                '  INSTALLNUM=true\n' +
                'fi\n' +
                'echo "INSTALLNUM=$INSTALLNUM" >> env.properties\n\n' +
                'INSTALLRMCA=false\n' +
                'if [ -n "${RMCA_TARGETHOST}" ]; then\n' +
                '  INSTALLRMCA=true\n' +
                'fi\n' +
                'echo "INSTALLRMCA=$INSTALLRMCA" >> env.properties'
    }

    private void addConditionalStepWithRegExpression(String label, String tpgName,
                                                     Map<String, String> predefinedPropsMap) {
        job.with {
            steps {
                conditionalSteps {
                    condition {
                        expression('true', label)
                    }
                    steps {
                        downstreamParameterized {
                            trigger(tpgName + '_targethost_install') {
                                block {
                                    buildStepFailure('FAILURE')
                                    failure('FAILURE')
                                    unstable('UNSTABLE')
                                }
                                parameters {
                                    predefinedProps(predefinedPropsMap)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void addConditionalStepWithRegExpressionOnRemote(String label, String tpgName,
                                                             String jenkins, Map<String, String> predefinedPropsMap) {
        job.with {
            steps {
                conditionalSteps {
                    condition {
                        expression('true', label)
                    }
                    steps {
                        remoteTrigger(jenkins, tpgName + '_targethost_install') {
                            blockBuildUntilComplete(true)
                            pollInterval(10)
                            parameters(predefinedPropsMap)
                        }
                    }
                }
            }
        }
    }

    private String getTargetHostDescription(String tpg) {
        return "The machine that should be installed with "+ tpg +". <b>Leave empty if you don't want to install this DV</b>"
    }

    private String getVersionDescription(String tpg) {
        return "Version of " + tpg + " to install. Use LATEST to get the latest working SNAPSHOT. <br><br><br>"
    }

}
