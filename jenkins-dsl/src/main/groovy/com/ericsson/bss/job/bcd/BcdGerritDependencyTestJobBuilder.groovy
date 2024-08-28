package com.ericsson.bss.job.bcd

import com.ericsson.bss.job.GerritDependencyTestJobBuilder
import com.ericsson.bss.project.Bcd

import javaposse.jobdsl.dsl.Job

public class BcdGerritDependencyTestJobBuilder extends GerritDependencyTestJobBuilder {

    @Override
    public void initProject(Job job) {
        super.initProject(job)
        job.with { jdk(Bcd.JDK_VERSION) }
    }
}
