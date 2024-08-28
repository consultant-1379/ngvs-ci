package com.ericsson.bss.job.cil

import com.ericsson.bss.job.TargethostInstallJobBuilder

class CilTargethostInstallJobBuilder extends TargethostInstallJobBuilder {

    @Override
    protected String getOptionalTapasParameters() {
        return '--define=__NO_OF_CIL__=${NO_OF_CIL} \\\n'
    }

    @Override
    protected String getAdditionalMultipleHostShell() {
        return  '### targethost list begin\n' +
                'FILENAME="${TARGETHOST//;/_}"\n' +
                'NO_OF_CIL="\$(echo \${TARGETHOST} | awk --field-separator=";" \'{ printf NF }\')"\n' +
                'TARGETHOST="\$(printf \'[%s]\' \$(echo \${TARGETHOST} | sed -e \'s/;/\\n/g\' | xargs printf \'{\\"target.host\":\"%s\"},\' ' +
                                                                                                                       '| sed s\'/.\$//\'))"\n' +
                '### end\n\n' +
                '\n'
    }
}
