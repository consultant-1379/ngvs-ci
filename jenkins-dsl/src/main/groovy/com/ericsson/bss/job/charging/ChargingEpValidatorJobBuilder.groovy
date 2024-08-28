package com.ericsson.bss.job.charging

import com.ericsson.bss.job.EpValidatorJobBuilder

class ChargingEpValidatorJobBuilder extends EpValidatorJobBuilder {

    private List installType = ['full', 'core', 'access', 'full+dlb', 'dlb']

    @Override
    protected void setExtraOptions() {
        job.with {
            parameters {
                choiceParam('INSTALL_TYPE', installType, 'Which type of installation to be performed. ' +
                        'Full will install both CHARGINGCORE and CHARGINGACCESS \n' +
                        'Full+dlb will install both CHARGINGCORE, CHARGINGACCESS and CHARGINGDLB\n' +
                        'Core and access installtypes needs the other hosts to be specified to be correctly ' +
                        'configured but will NOT install anything on it. Dlb installtypes needs core hots to be ' +
                        'specified to be correctly configured but will NOT install anything on it.')
                stringParam('DLBHOST', '', 'Host that should be installed with DLB')
                stringParam('DLBVERSION', 'LATEST', 'Version of DLB that should be installed')
                stringParam('DLBDVFILE', '', 'Optional. A custom file to use instead of an artifact from ARM (charging-*-dv.tar.gz). The file must be ' +
                        'accessible from Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL. Leave empty to download the specified VERSION from ARM.')
                stringParam('DLBEPFILE', '', 'A custom file to use instead of an artifact from ARM (*.tar.gz). The file must be accessible from Jenkins as a ' +
                        'filepath (ie /workarea/.. or /proj/..) or a URL. Leave empty to download the specified VERSION from ARM.')
                stringParam('DLBEPDELTA', '', 'Delta version of the EP, e.g (EP04A)')
            }
        }
    }

    @Override
    protected String getOptionalTapasParameters() {
        return '--define=__DLBHOST__="\${DLBHOST}" \\\n' +
               '--define=__DLBEPFILE__="\${DLBEPFILE}" \\\n' +
               '--define=__DLBEPDELTA__="\${DLBEPDELTA}" \\\n'
    }

    @Override
    protected preparePredefinedProp() {
        predefinedProperties = [TARGETHOST: '\$TARGETHOST',
                                TARGETHOST2: '\$TARGETHOST2',
                                TARGETHOST3: '\$DLBHOST',
                                MSV: '\$MSV',
                                COREOVFVERSION: '\$VERSION',
                                ACCESSOVFVERSION: '\$VERSION2',
                                DLBOVFVERSION: '\$DLBVERSION',
                                VMAPI_PREFIX: '\$VMAPI_PREFIX',
                                PRODUCT: '\$PRODUCT',
                                DO_ROLLBACK: 'true',
                                DLBHOST: '\$DLBHOST',
                                DLBDVFILE: '\$DLBDVFILE',
                                INSTALL_TYPE: '\$INSTALL_TYPE']
    }
}
