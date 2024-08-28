package com.ericsson.bss.job.washingmachine.utils

import javaposse.jobdsl.dsl.DslFactory
import com.ericsson.bss.util.JobContext

import javax.management.BadAttributeValueExpException

class JobEnabledScriptBuilder {

    private static final PROJECT_NAME_ANCHOR = '<PROJECT_NAME>'

    DslFactory dslFactory
    String projectName

    public static JobEnabledScriptBuilder newBuilder(DslFactory dslFactory, String projectName) {
        return new JobEnabledScriptBuilder([dslFactory: dslFactory, projectName: projectName])
    }

    public static JobEnabledScriptBuilder newBuilder(Map params) {
        return new JobEnabledScriptBuilder([projectName: params.projectName])
    }

    public String build() {
        if (!projectName) {
            throw new BadAttributeValueExpException("Project name cannot be null or empty")
        }
        return JobContext.getDSLFactory().readFileFromWorkspace('scripts/washingmachine/job_enabled_param.groovy')
                .replace(PROJECT_NAME_ANCHOR, projectName)
    }
}
