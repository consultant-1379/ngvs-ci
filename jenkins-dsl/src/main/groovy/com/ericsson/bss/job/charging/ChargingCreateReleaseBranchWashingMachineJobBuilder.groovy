package com.ericsson.bss.job.charging

import com.ericsson.bss.job.CreateReleaseBranchWashingMachineJobBuilder

public class ChargingCreateReleaseBranchWashingMachineJobBuilder extends CreateReleaseBranchWashingMachineJobBuilder {

    @Override
    protected void getExtraOptions() {
        job.with {
            parameters {
                stringParam('SERVICERPMVERSION', '', 'Servicelogics rpm version to use.')
                stringParam('DLBHOST', '', 'Machine that should be installed with DLB.')
                stringParam('DLBVERSION', '', 'Version of the DLB to use.')
            }
        }
    }

    @Override
    protected String getExtraParameters() {
        return "SERVICERPMVERSION = build.getEnvironment(listener).get('SERVICERPMVERSION')\n" +
               "DLBHOST = build.getEnvironment(listener).get('DLBHOST')\n" +
               "DLBVERSION = build.getEnvironment(listener).get('DLBVERSION')\n"
    }

    @Override
    protected String getXmlExtraParameters() {
        return ", servicerpmversion: SERVICERPMVERSION, dlbhost: DLBHOST, dlbversion: DLBVERSION"
    }
}
