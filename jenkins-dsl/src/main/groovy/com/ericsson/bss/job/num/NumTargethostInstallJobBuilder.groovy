package com.ericsson.bss.job.num

import com.ericsson.bss.job.TargethostInstallJobBuilder

class NumTargethostInstallJobBuilder extends TargethostInstallJobBuilder {

    @Override
    protected void setExtraOptions() {
        job.with {
            parameters {
                choiceParam('ADDSITEPARTITION', booleanFalseTrueList, 'Add sitepartitions scripts to CIL. ')
            }
        }
    }

    @Override
    protected String getOptionalTapasParameters() {
        return '--define=__ADDSITEPARTITION__="\${ADDSITEPARTITION}" \\\n'
    }

}
