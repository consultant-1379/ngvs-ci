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
package com.ericsson.bss.job.diameter

import javaposse.jobdsl.dsl.Job

import com.ericsson.bss.AbstractJobBuilder

class DiameterNativeJobBuilder  extends AbstractJobBuilder {

    public Job build() {
        runXvfb = false //No support in SunOS and Sparc

        initProject(dslFactory.matrixJob(jobName))
        addNativeJobConfig()

        return job
    }

    protected String getScriptToSetDynamicTimeout() {
        return "" //Groovy will not work on SunOS and Sparc
    }

    private void addNativeJobConfig() {
        job.with {

            description('<h2>Will compile the native libraries</h2>\n' +
                '<p>Jobs will be spread to specific slaves to be able to compile the native libraries.</p>')

            parameters {
                stringParam('BRANCH', 'master', 'Git branch to be built.')
            }

            environmentVariables { env('JOB_TIMEOUT', timeoutForJob) }

            addGitRepository(gerritName, '\${BRANCH}')

            axes {
                label('label', 'Linux_redhat_6.2_x86_64_mesos', 'SunOS_5.10_i386', 'SunOS_5.10_sparc')
            }

            addTimeoutConfig()

            steps {
                shell(getNativeCompileCommand())
            }

            publishers {
                wsCleanup()
            }
        }
    }

    @Override
    protected void archiveArtifacts() {
        job.with{
            publishers {
                archiveArtifacts {
                    allowEmpty(true)
                    pattern(getJavaCrashLogToArchive() + ',' + getDiameterArtifactsToArchive())
                }
            }
        }
    }

    private String getNativeCompileCommand() {
        return '#!/bin/ksh\n' +
                '\n' +
                'set -e\n' +
                'set -x\n' +
                '\n' +
                'if [[ `uname -s` = "Linux" ]]; then\n' +
                '    export JAVA_HOME=/opt/local/dev_tools/java/x64/latest-1.6\n' +
                'fi\n' +
                'if [[ `uname -s` = "SunOS" ]]; then\n' +
                '    export JAVA_HOME=/opt/local/dev_tools/java/latest-1.6\n' +
                'fi\n' +
                '\n' +
                '/opt/local/dev_tools/ant/1.8.2/bin/ant compile-native\n'
    }

    private String getJavaCrashLogToArchive() {
        return '**/hs_err_pid*.log'
    }

    private String getDiameterArtifactsToArchive() {
        return '*/target/liberisctp*.so'
    }
}
