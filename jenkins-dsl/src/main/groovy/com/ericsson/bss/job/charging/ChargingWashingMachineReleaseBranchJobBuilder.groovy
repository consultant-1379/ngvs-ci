package com.ericsson.bss.job.charging

import com.ericsson.bss.job.washingmachine.WashingMachineReleaseBranchJobBuilder

class ChargingWashingMachineReleaseBranchJobBuilder extends WashingMachineReleaseBranchJobBuilder {

    @Override
    protected void getExtraBuildParameters() {
        if (isRpm) {
            job.with {
                parameters {
                    stringParam('SERVICERPMVERSION', releaseBranch.@servicerpmversion, '')
                    stringParam('DLBHOST', releaseBranch.@dlbhost, '')
                    stringParam('DLBVERSION', releaseBranch.@dlbversion, '')
                }
            }
        }
    }

    @Override
    protected String getExtraTapasParameters() {
        if (isRpm) {
            return '--define=__SERVICERPMVERSION__=${SERVICERPMVERSION} \\\n' +
                   '--define=__VMAPI_PROFILE_PREFIX__="charging." \\\n' +
                   '--define=__DLBHOST__="${DLBHOST}" \\\n' +
                   '--define=__DLBRPMVERSION__="${DLBVERSION}" \\\n'
        }
        return ""
    }

}

