package com.ericsson.bss.job.washingmachine.utils

import javaposse.jobdsl.dsl.DslFactory
import com.ericsson.bss.util.JobContext

import javax.management.BadAttributeValueExpException

class SelectedStringScriptBuilder {

    private static final PARAMETER_NAME = '<PARAMETER_NAME>'
    private static final PROJECT_NAME = '<PROJECT_NAME>'

    DslFactory dslFactory
    String parameterName
    String projectName

    public static SelectedStringScriptBuilder newBuilder(DslFactory dslFactory, String parameterName, String projectName) {
        return new SelectedStringScriptBuilder([dslFactory: dslFactory, parameterName: parameterName, projectName: projectName])
    }

    public static SelectedStringScriptBuilder newBuilder(Map params) {
        return new SelectedStringScriptBuilder([parameterName: params.parameterName, projectName: params.projectName])
    }

    public String build() {
        if (!parameterName) {
            throw new BadAttributeValueExpException("Parameter name cannot be null or empty")
        }
        if (!projectName) {
            throw new BadAttributeValueExpException("Project name cannot be null or empty")
        }
        return JobContext.getDSLFactory().readFileFromWorkspace('scripts/washingmachine/selected_string_param.groovy')
                .replace(PROJECT_NAME, projectName)
                .replace(PARAMETER_NAME, parameterName)
    }
}
