package com.ericsson.bss.job.mapt

import com.ericsson.bss.job.TargethostInstallJobBuilder

class MaptTargethostInstallJobBuilder extends TargethostInstallJobBuilder {

    @Override
    protected void setExtraOptions() {
        job.with {
            parameters {
                stringParam('HLRHOST', '', "The hostname of the HLR.")
            }
        }
    }

    @Override
    protected String getOptionalTapasParameters() {
        return '--define=__HLRHOST__="\${HLRHOST}" \\\n'
    }
}