package com.ericsson.bss.job.chargingcore

import com.ericsson.bss.job.custom.InstallJobBuilder

import javaposse.jobdsl.dsl.Job

public class ChargingCoreIntegrationTestTraceInstallJobBuilder extends InstallJobBuilder {

    @Override
    public Job build() {
        Job job = super.build()

        job.with {
            logRotator(5, 5, 5, 5)
        }

        return job
    }
}
