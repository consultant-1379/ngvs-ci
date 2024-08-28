//**********************************************************************
// Copyright (c) 2016 Telefonaktiebolaget LM Ericsson, Sweden.
// All rights reserved.
// The Copyright to the computer program(s) herein is the property of
// Telefonaktiebolaget LM Ericsson, Sweden.
// The program(s) may be used and/or copied with the written permission
// from Telefonaktiebolaget LM Ericsson or in accordance with the terms
// and conditions stipulated in the agreement/contract under which the
// program(s) have been supplied.
// **********************************************************************
package com.ericsson.bss.job.cake

import com.ericsson.bss.job.SonarJobBuilder

class CakeSonarJobBuilder extends SonarJobBuilder {
    private String mailRecipients

    @Override
    protected String getSonarAdditionalProperties() {
        return super.getSonarAdditionalProperties() +
                ' -Dsonar.timemachine.period5=14 -Dsonar.timemachine.period4=7'
    }

    @Override
    protected void addExtendableEmail() {
        String recipientsList = '\$DEFAULT_RECIPIENTS'

        if (mailRecipients != null) {
            recipientsList += ', ' + mailRecipients
        }

        super.addExtendableEmail(recipientsList)
    }
}
