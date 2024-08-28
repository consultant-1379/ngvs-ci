package com.ericsson.bss.job.bcd

import com.ericsson.bss.job.SonarJobBuilder
import com.ericsson.bss.project.Bcd

import javaposse.jobdsl.dsl.Job

class BcdSonarJobBuilder extends SonarJobBuilder {

    @Override
    public void initProject(Job job) {
        super.initProject(job)
        job.with { jdk(Bcd.JDK_VERSION) }
    }
}
