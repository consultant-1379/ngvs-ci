package com.ericsson.bss.job.coba

import com.ericsson.bss.job.TargethostInstallJobBuilder

class CobaTargethostInstallJobBuilder extends TargethostInstallJobBuilder {

    @Override
    protected void setExtraOptions() {
        job.with {
            parameters {
                stringParam('APPGROUP', 'COBA1', 'What appgroup should be used.')
                stringParam('MSG', '', "<b>Optional.</b> Default is empty and will use InstallActiveMQ workaround." +
                                       "</br> Put a MSG host if you have one and wan't to use it during the" +
                                       "installation. </br><b>Do NOT put your COBA host here!</b>")
            }
        }
        addListChoiceParam('ACTIVATE_ALL_FC', booleanFalseTrueList, 'Should all functions be activated')
    }

    @Override
    protected String getOptionalTapasParameters() {
        return '--define=__APPGROUP__="\${APPGROUP}" \\\n' +
               '--define=__MSG__="\${MSG}" \\\n' +
               '--define=__ACTIVATE_ALL_FC__=${ACTIVATE_ALL_FC} \\\n'
    }

}
