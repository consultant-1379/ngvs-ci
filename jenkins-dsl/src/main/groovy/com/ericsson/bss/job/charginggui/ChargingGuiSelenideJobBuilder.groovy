package com.ericsson.bss.job.charginggui

import javaposse.jobdsl.dsl.Job

import com.ericsson.bss.AbstractJobBuilder

public class ChargingGuiSelenideJobBuilder extends AbstractJobBuilder{

    private String gerritName
    private static final String JOB_DESCRIPTION = "<h2>This job execute charging gui selenide testcases.</h2>"
    private String selenideTestScripts = 'scripts/charging_gui_selenide_test.sh'

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addSelenideTestConfig()
        return job
    }

    public void addSelenideTestConfig(){
        job.with {
            description(DSL_DESCRIPTION + JOB_DESCRIPTION)
            addGitRepository(gerritName)
            triggers { cron('H H(0-7) * * *') }
            steps {
                shell(symlinkMesosWorkSpace())
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
                shell(dslFactory.readFileFromWorkspace(selenideTestScripts))
            }

            injectEnv(getInjectVariables())
            addTimeoutConfig()
            publishers { archiveJunit('selenidetest/target/surefire-reports/*.xml')}
        }
    }
}
