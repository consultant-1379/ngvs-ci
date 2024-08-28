package com.ericsson.bss.job.washingmachine.utils

import javaposse.jobdsl.dsl.DslFactory
import com.ericsson.bss.util.JobContext

import javax.management.BadAttributeValueExpException

class EnabledHostsScriptBuilder {

    private static final PROJECT_NAME_ANCHOR = '<PROJECT_NAME>'

    DslFactory dslFactory
    String projectName

    public static EnabledHostsScriptBuilder newBuilder(DslFactory dslFactory, String projectName) {
        return new EnabledHostsScriptBuilder([dslFactory: dslFactory, projectName: projectName])
    }

    public static EnabledHostsScriptBuilder newBuilder(Map params) {
        return new EnabledHostsScriptBuilder([projectName: params.projectName])
    }

    public String build() {
        if (!projectName) {
            throw new BadAttributeValueExpException("Project name cannot be null or empty")
        }
        return JobContext.getDSLFactory().readFileFromWorkspace('scripts/washingmachine/enabled_hosts_param.groovy')
                .replace(PROJECT_NAME_ANCHOR, projectName)
    }
}
