package com.ericsson.bss.job.chargingaccess

import com.ericsson.bss.job.TargethostInstallRpmJobBuilder

public class ChargingAccessTargethostInstallRpmJobBuilder extends TargethostInstallRpmJobBuilder {

    private String parameterName = "TARGETHOST2"

    @Override
    protected void setInputParameters() {
        super.setInputParameters()
        job.with {
            parameters {
                stringParam('CONTROLLER_RPM', 'LATEST',
                        'The RPM to install. LATEST or a specific version will fetch from ' +
                                'ARM. A file path accessible from Jenkins or a URL to a RPM will ' +
                                'use the one specified.')
                stringParam('SERVICELOGICS_RPM', 'LATEST',
                        'The RPM to install. LATEST or a specific version will fetch from ' +
                                'ARM. A file path accessible from Jenkins or a URL to a RPM will ' +
                                'use the one specified.')
            }
        }
    }

    @Override
    protected String getTapasParameters() {
        String params = super.getTapasParameters()
        params += '--define=__ACCESS_CONTROLLER_RPM__="${CONTROLLER_RPM}" \\\n' +
                '--define=__ACCESS_SERVICELOGICS_RPM__="${SERVICELOGICS_RPM}" \\\n'
        return params
    }
}