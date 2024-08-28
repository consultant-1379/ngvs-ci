package com.ericsson.bss.job.rmca

import com.ericsson.bss.job.TargethostInstallJobBuilder

class RmcaTargethostInstallJobBuilder extends TargethostInstallJobBuilder {

    @Override
    protected void setExtraOptions() {
            job.with {
                parameters {
                    choiceParam('RUN_SIMULATORS', booleanTrueFalseList, 'If the job should start simulators for CHA, COBA and NTF.')
                }
            }
            addListChoiceParam('ACTIVATE_ALL_FC', booleanFalseTrueList, 'Should all functions be activated')
            addListChoiceParam('INSTALL_TESTDATA', booleanFalseTrueList, 'Should testdata be installed')
    }

    @Override
    protected String getOptionalTapasParameters() {
            return '--define=__RUN_SIMULATORS__=${RUN_SIMULATORS} \\\n' +
                   '--define=__ACTIVATE_ALL_FC__=${ACTIVATE_ALL_FC} \\\n' +
                   '--define=__INSTALL_TESTDATA__=${INSTALL_TESTDATA} \\\n'
    }

}
