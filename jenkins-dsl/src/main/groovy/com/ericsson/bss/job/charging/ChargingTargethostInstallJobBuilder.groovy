package com.ericsson.bss.job.charging

import com.ericsson.bss.job.TargethostInstallJobBuilder

class ChargingTargethostInstallJobBuilder extends TargethostInstallJobBuilder {

    @Override
    protected String hostResourceProfile() {
        String resourceProfileShell = 'INSTALL_DLB=false\n' +
        'NUMBER_OF_TARGETHOSTS="$(echo ${TARGETHOST} | awk --field-separator=";" \'{ printf NF }\')"\n' +
        'TARGETHOSTS_JSON="$(printf \'[%s]\' $(echo ${TARGETHOST} | sed -e \'s/;/\\n/g\' ' +
        '| xargs printf \'{\\"target.host\\":\\"%s\\"},\' | sed s\'/.$//\'))"\n' +
        'NUMBER_OF_TARGETHOSTS2="$(echo ${TARGETHOST2} | awk --field-separator=";" \'{ printf NF }\')"\n' +
        'TARGETHOSTS2_JSON="$(printf \'[%s]\' $(echo ${TARGETHOST2} | sed -e \'s/;/\\n/g\' ' +
        '| xargs printf \'{\\"target.host2\\":\\"%s\\"},\' | sed s\'/.$//\'))"\n' +
        'NUMBER_OF_TARGETHOSTS3="$(echo ${TARGETHOST3} | awk --field-separator=";" \'{ printf NF }\')"\n' +
        'TARGETHOSTS3_JSON="$(printf \'[%s]\' $(echo ${TARGETHOST3} | sed -e \'s/;/\\n/g\' ' +
        '| xargs printf \'{\\"target.host3\\":\\"%s\\"},\' | sed s\'/.$//\'))"\n' +
        'OVFPACNAME="' + ovfPacName[0] + '"\n' +
        'if [[ "${INSTALLTYPE}" = "access" ]]; then\n' +
        '    OVFPACNAME="' + ovfPacName[1] + '"\n' +
        '    CUSTOMRAM="${CUSTOMRAM2}"\n' +
        '    CUSTOMCPU="${CUSTOMCPU2}"\n' +
        '    VERSION=${VERSION2}\n' +
        '   NUMBER_OF_TARGETHOSTS="$(echo ${TARGETHOST2} | awk --field-separator=";" \'{ printf NF }\')"\n' +
        '   TARGETHOSTS_JSON="$(printf \'[%s]\' $(echo ${TARGETHOST2} | sed -e \'s/;/\\n/g\' ' +
            '| xargs printf \'{\\"target.host\\":\\"%s\\"},\' | sed s\'/.$//\'))"\n' +
        '   NUMBER_OF_TARGETHOSTS2="$(echo ${TARGETHOST} | awk --field-separator=";" \'{ printf NF }\')"\n' +
        '   TARGETHOSTS2_JSON="$(printf \'[%s]\' $(echo ${TARGETHOST} | sed -e \'s/;/\\n/g\' ' +
            '| xargs printf \'{\\"target.host2\\":\\"%s\\"},\' | sed s\'/.$//\'))"\n' +
        'elif [[ "${INSTALLTYPE}" = "dlb" ]]; then\n' +
        '    OVFPACNAME="' + ovfPacName[2] + '"\n' +
        '    CUSTOMRAM="${CUSTOMRAM3}"\n' +
        '    CUSTOMCPU="${CUSTOMCPU3}"\n' +
        '   NUMBER_OF_TARGETHOSTS="$(echo ${TARGETHOST3} | awk --field-separator=";" \'{ printf NF }\')"\n' +
        '   TARGETHOSTS_JSON="$(printf \'[%s]\' $(echo ${TARGETHOST3} | sed -e \'s/;/\\n/g\' ' +
            '| xargs printf \'{\\"target.host\\":\\"%s\\"},\' | sed s\'/.$//\'))"\n' +
        '   NUMBER_OF_TARGETHOSTS2="$(echo ${TARGETHOST} | awk --field-separator=";" \'{ printf NF }\')"\n' +
        '   TARGETHOSTS2_JSON="$(printf \'[%s]\' $(echo ${TARGETHOST} | sed -e \'s/;/\\n/g\' ' +
            '| xargs printf \'{\\"target.host2\\":\\"%s\\"},\' | sed s\'/.$//\'))"\n' +
        '    VERSION=${VERSION3}\n' +
        'elif [[ "${INSTALLTYPE}" = "full+dlb" ]]; then\n' +
        '    INSTALL_DLB=true\n' +
        'fi\n'

        return resourceProfileShell
    }

    @Override
    protected String getTapasConfigSettings() {
        String config_shell = 'BASE_CONFIG_FILE="${PRODUCT_CONFIG_DIR}' + suite + '"\n' +
                              'CONFIG_FILE="/proj/eta-automation/tapas/sessions/' + tapasProjectDestinationName + '/' + suiteFile + '"\n'
        if (versionLocation.size() > 1) {
            config_shell +=   '\n' +
                              'if [[ "${INSTALLTYPE}" = *"full"* ]]; then\n' +
                              '    BASE_CONFIG_FILE="${PRODUCT_CONFIG_DIR}' + suiteTwoHosts + '"\n' +
                              '    CONFIG_FILE="/proj/eta-automation/tapas/sessions/' + tapasProjectDestinationName + '/' + suiteFileTwoHost + '"\n' +
                              'fi\n'
        }
        return config_shell
    }

    protected String getOptionalTapasParameters() {
        String additionalParameters = ""
        if (versionLocation.size() > 1) {
            additionalParameters = '--define=__OVFPACNAME__=${OVFPACNAME} \\\n' +
                                   '--define=__VARIANT__=${INSTALLTYPE} \\\n' +
                                   '--define=__INSTALL_DLB__=${INSTALL_DLB} \\\n'
            if (useMultipleTargethosts) {
                additionalParameters += '--define=__NUMBER_OF_TARGETHOSTS__=${NUMBER_OF_TARGETHOSTS} \\\n'
                for (int i = 2; i < versionLocation.size() + 1;i++) {
                    additionalParameters += '--define=__NUMBER_OF_TARGETHOSTS' + i + '__=${NUMBER_OF_TARGETHOSTS' + i + '} \\\n'
                }
            }
        }
        return additionalParameters
    }

}