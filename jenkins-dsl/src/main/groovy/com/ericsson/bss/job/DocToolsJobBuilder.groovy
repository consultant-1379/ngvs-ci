package com.ericsson.bss.job

import javaposse.jobdsl.dsl.Job

import com.ericsson.bss.AbstractJobBuilder

class DocToolsJobBuilder extends AbstractJobBuilder{
    protected String projectName

    public Job build(){
        initProject(dslFactory.freeStyleJob(jobName))
        addToolsDocConfig()
        return job
    }

    public void addToolsDocConfig(){
        job.with {
            String jobDescription = "" +
            "<h2>Generate a document with all ETA controlled tools' versions.</h2>"

            description(DSL_DESCRIPTION + jobDescription)
            steps {
                shell(dslFactory.readFileFromWorkspace('scripts/generate_env_tools_doc.sh'))
            }
            injectEnv(getInjectVariables())
            addTimeoutConfig()
            publishers {
                archiveArtifacts('tools.txt')
                wsCleanup()
            }
        }
    }
}
