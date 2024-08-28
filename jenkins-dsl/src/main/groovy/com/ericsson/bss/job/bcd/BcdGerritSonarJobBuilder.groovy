package com.ericsson.bss.job.bcd

import com.ericsson.bss.job.GerritSonarJobBuilder
import com.ericsson.bss.project.Bcd

import javaposse.jobdsl.dsl.Job

class BcdGerritSonarJobBuilder extends GerritSonarJobBuilder {

    @Override
    public void initProject(Job job) {
        super.initProject(job)
        job.with { jdk(Bcd.JDK_VERSION) }
    }
}
