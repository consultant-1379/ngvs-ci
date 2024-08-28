package com.ericsson.bss.job

import javaposse.jobdsl.dsl.Job

public class MvnGerritUnitTestJobBuilder extends GerritUnitTestJobBuilder {

    protected String projectName

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))

        addUnitTestConfig()

        super.gerritTriggerSilent()

        return job
    }

    @Override
    protected void extraShellSteps() {
        job.with {
            steps {
                shell(removeOldArtifacts())
                shell(mavenBuildCommand())
                shell(super.junitPublisherWorkaround())
            }
        }
    }

    protected String mavenBuildCommand() {
        String cmd = getShellCommentDescription("Maven build command") +
                "mvn \\\n" +
                "clean install \\\n"

        if (mavenProjectLocation){
            cmd += " -f " + mavenProjectLocation + " \\\n"
        }

        if (profilesToBeUsed != null && profilesToBeUsed != "") {
            cmd += '-P' + profilesToBeUsed + ' \\\n'
        }

        if (runParallelThreads) {
            cmd +=  "-DparallelTests -T 4 \\\n"
        }
        cmd += "-Dsurefire.useFile=false \\\n" +
                getMavenGeneralBuildParameters()

        return cmd
    }

    @Override
    protected void setPublishers() {
        job.with {
            publishers { archiveJunit('**/surefire-reports/*.xml') }
        }
        super.setPublishers()
    }
}
