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
package com.ericsson.bss.job.chargingcore

import com.ericsson.bss.job.GerritUnitTestCheckstyleJobBuilder

class ChargingCoreGerritUnitTestCheckstyleJobBuilder extends GerritUnitTestCheckstyleJobBuilder {

    @Override
    protected String gerritFeedbackFail() {

        if (gerritName.equals('charging/com.ericsson.bss.rm.charging.integrationtest')) {

            String integrationtestFeedback = '''ssh -o BatchMode=yes -p 29418 -l <GERRIT_USER> <GERRIT_SERVER> gerrit review --project ${GERRIT_PROJECT} <GERRIT_NOTIFY> -m '"Unit tests FAILED, '${BUILD_URL}'

Please go to <HELP_LINK> to find out if you introduced the error or if it is an existing error in the IntegrationTest."' --verified -1 ${GERRIT_PATCHSET_REVISION}'''
                    .replace("<GERRIT_USER>", jenkinsUnitTestUser)
                    .replace("<GERRIT_SERVER>", gerritServer)
                    .replace("<HELP_LINK>", "https://eta.epk.ericsson.se/maven-sites/latest/com.ericsson.bss.rm.charging/parent/integrationtest/errors.html")
                    .replace("<GERRIT_NOTIFY>", getNotifySetting())

            if (isUsingLabelForReviews()) {
                integrationtestFeedback = integrationtestFeedback
                        .replace("-l " + jenkinsUnitTestUser, "")
                        .replace("--verified ", "--label Unit-Test=")
            }

            return integrationtestFeedback
        }
        else {
            super.gerritFeedbackFail()
        }
    }

    @Override
    protected getInjectVariables() {
        Map env_list = super.getInjectVariables()

        env_list.put("GRAPHVIZ_DOT", GRAPHVIZ_HOME + "/dot")
        env_list['PATH'] += ":" + GRAPHVIZ_HOME
        return env_list
    }
}
