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

import com.ericsson.bss.job.DeployJobBuilder

class DiameterDeliveryJobBuilder extends DeployJobBuilder {

    protected String branch = '\${BRANCH}'

    @Override
    public Job build() {
        mavenTarget = "clean install"

        super.build()

        addDeliveryJobConfig()

        return job
    }

    private void addDeliveryJobConfig() {
        job.with {

            description(DSL_DESCRIPTION + '<h2>Create a delivery for Diameter</h2>\n' +
                    '<p>This job will compile the code.<br/>\n' +
                    '<p>The job will then trigger the diameter native jobs to build the libraries on the different architectures.<br/>\n' +
                    '<p>Last step in the process is that the build package job is triggered. ' +
                    'That job will collect artifacts from both this job and the native jobs and build a deliverable package.</p>\n')

            parameters {
                stringParam('PRODUCT_REVISION', 'BETA', 'Revision will be added to the target zip-file and as the target release for the EForge file' +
                        'release upload if the package is promoted.')
                stringParam('BRANCH', 'master', 'Git branch to be built.')
            }
        }
    }

    @Override
    protected void archiveArtifacts() {
        job.with{
            publishers {
                archiveArtifacts {
                    allowEmpty(true)
                    pattern(getJavaCrashLogToArchive() + ',' + getDiameterPackageToArchive())
                }
            }
        }
        //To get the correct order we add 'joinProjects' and 'triggerDownstream' after archive.
        joinProjects()
        triggerDownstreamedJob()
    }

    //TODO: Used in multiple places, should be moved up
    private String getJavaCrashLogToArchive() {
        return '**/hs_err_pid*.log'
    }

    private String getDiameterPackageToArchive() {
        return '*/target/*.jar'
    }

    private void joinProjects() {
        job.with{
            publishers {
                joinTrigger {
                    publishers {
                        downstreamParameterized {
                            trigger('diameter_base_java-source_package') {
                                parameters {
                                    gitRevision(false)
                                    currentBuild()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void triggerDownstreamedJob() {
        job.with{
            publishers {
                downstreamParameterized {
                    trigger('diameter_base_java-source_native_deploy') {
                        condition('SUCCESS')
                        parameters {
                            currentBuild()
                            gitRevision(false)
                        }
                    }
                }
            }
        }
    }
}
