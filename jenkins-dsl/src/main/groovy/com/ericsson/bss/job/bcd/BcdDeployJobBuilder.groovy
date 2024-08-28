package com.ericsson.bss.job.bcd

import com.ericsson.bss.job.DeployJobBuilder
import com.ericsson.bss.project.Bcd

import javaposse.jobdsl.dsl.Job

class BcdDeployJobBuilder extends DeployJobBuilder {

    @Override
    public void initProject(Job job) {
        super.initProject(job)
        job.with { jdk(Bcd.JDK_VERSION) }
    }

    @Override
    protected void addDeployConfig() {
        super.addDeployConfig()
        if (gerritName == 'bcd/integration') {
            job.with {
                description(dslFactory.readFileFromWorkspace('src/main/resources/bcd/IntegrationDescription.txt') +
                        '\n\n' +
                        DSL_DESCRIPTION +
                        JOB_DESCRIPTION)
            }
        }
        else if (gerritName == 'bcd/devbuild') {
            job.with {
                description(dslFactory.readFileFromWorkspace('src/main/resources/bcd/TestClientsimulatorDescription.txt') +
                        '\n\n' +
                        DSL_DESCRIPTION +
                        JOB_DESCRIPTION)
            }
        }
    }

    @Override
    protected void addMavenRelease() {
        if (gerritName != 'bcd/devbuild') {
            super.addMavenRelease()
        }
    }

    @Override
    protected void archiveArtifacts() {
        if (gerritName == 'bcd/agent') {
            setBcdArchivePublisher('bcdclient/target/com.ericsson.rm.bcd.agent-*-delivery.zip, ' +
                    'target/bcdclient-*-sources.jar, ' +
                    'target/agent-*-sourcerelease.tar.gz'
                    )
        }
        else if (gerritName == 'bcd/integration') {
            setBcdArchivePublisher('server/delivery/*/target/*CXP*.tar.gz, server/build/target/build-*-bin.zip')
        }
        else if (gerritName == 'bcd/devbuild') {
            setBcdArchivePublisher('bcd.client.sim/target/clientsimulator-*-assembly.zip')
        }
        else {
            super.archiveArtifacts()
        }
    }

    private setBcdArchivePublisher(String patternToSet) {
        job.with{
            publishers {
                archiveArtifacts {
                    allowEmpty(true)
                    pattern(patternToSet + ', **/hs_err_pid*.log')
                }
            }
        }
    }
}
