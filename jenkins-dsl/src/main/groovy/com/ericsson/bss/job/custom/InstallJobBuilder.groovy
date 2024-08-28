package com.ericsson.bss.job.custom

import com.ericsson.bss.job.DeployJobBuilder
import javaposse.jobdsl.dsl.Job

public class InstallJobBuilder extends DeployJobBuilder {

    @Override
    public Job build() {
        mavenTarget = "clean install"
        return super.build()
    }

    @Override
    protected void addBlameMail() {
    }

    @Override
    protected void addMavenRelease() {
    }
}
