package com.ericsson.bss.job.diameter

import com.ericsson.bss.job.DeployJobBuilder

public class DiameterDeployJobBuilder extends DeployJobBuilder {

    @Override
    protected void addMavenRelease() {
        //Diameter releases are done in a separate Jenkins job
    }

    @Override
    protected void archiveArtifacts() {
        job.with {
            publishers {
                archiveArtifacts {
                    allowEmpty(true)
                    pattern(getJavaCrashLogToArchive() + ',' + getDiameterArtifactsToArchive())
                }
            }
        }
    }

    private String getJavaCrashLogToArchive() {
        return '**/hs_err_pid*.log'
    }

    private String getDiameterArtifactsToArchive() {
        return '*/target/*.jar'
    }
}
