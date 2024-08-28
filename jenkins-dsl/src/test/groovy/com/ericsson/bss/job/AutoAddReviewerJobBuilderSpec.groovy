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
package com.ericsson.bss.job

import spock.lang.Specification
import com.ericsson.JobSpecMixin
import com.ericsson.bss.AbstractJobBuilder;

import javaposse.jobdsl.dsl.DslFactory;
import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent

@Mixin(JobSpecMixin)
class AutoAddReviewerJobBuilderSpec extends Specification
{

    private String[] reviewList = [
        "CFT group 1",
        "CFT group 2",
        "CFT group 3",
        "User A", ,
        "User B"
    ]

    void 'test XML output'() {
        given:
        JobSpecMixin jobSpecMixin = new JobSpecMixin()
        JobParent jobParent = jobSpecMixin.createJobParent()

        AutoAddReviewerJobBuilder builder = new AutoAddReviewerJobBuilder(
                reviewers: reviewList,
                jobName: "_gerrit_add_reviewers",
                gerritServer: "gerrit.epk.ericsson.se",
                gerritName: "nisse",
                injectPortAllocation: "null",
                dslFactory: jobParent
                )

        when:
        Job job = builder.build()

        then:

        //PrintXML
        new XmlNodePrinter(preserveWhitespace:true).print(job.node)

        //Review list
        String review = ""
        reviewList.each { review += " -a '${it}'" }

        job.node.builders.'hudson.tasks.Shell'.command.text().contains(review)

        //Job Configuration
        with(job.node) {
            name() == 'project'
            assignedNode.text() == AbstractJobBuilder.RESTRICT_LABEL_MESOS
        }
    }
}
