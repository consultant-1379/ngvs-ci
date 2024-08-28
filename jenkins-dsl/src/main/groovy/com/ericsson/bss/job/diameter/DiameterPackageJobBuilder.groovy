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

class DiameterPackageJobBuilder extends AbstractJobBuilder {

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addPackageJobConfig()

        return job
    }

    private void addPackageJobConfig() {
        job.with {

            description('<h2>Package artifacts from native and delivery job</h2>\n' +
                '<p>Will collect artifacts from upstream jobs and copy there artifcats.</p>\n' +
                '<p>The job will then trigger an Ant target to create a package.</p>')

            parameters {
                stringParam('BRANCH', 'master', 'Git branch to be built.')
            }

            addGitRepository(gerritName, '\${BRANCH}')

            injectEnv(getInjectVariables())

            addTimeoutConfig()

            steps {
                copyArtifacts('diameter_base_java-source_delivery') {
                    buildSelector {
                        upstreamBuild {
                            allowUpstreamDependencies(false)
                            fallbackToLastSuccessful(true)
                        }
                    }
                    includePatterns('**/target/*.jar')
                    targetDirectory('lib')
                    flatten()
                }
                copyArtifacts('diameter_base_java-source_native_deploy') {
                    buildSelector {
                        upstreamBuild {
                            allowUpstreamDependencies(false)
                            fallbackToLastSuccessful(true)
                        }
                    }
                    includePatterns('**/target/*.so')
                    targetDirectory('lib')
                    flatten()
                }

                ant {
                    antInstallation('Ant 1.8.2')
                    target('archive-delivery')
                }
            }

            publishers { wsCleanup() }
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
    }

    //TODO: Used in multiple places, should be moved up
    private String getJavaCrashLogToArchive() {
        return '**/hs_err_pid*.log'
    }

    private String getDiameterPackageToArchive() {
        return 'target/*.zip'
    }
}
