package com.ericsson.bss.decorators.scriptbuilders

import com.ericsson.bss.util.JobContext

import javax.management.BadAttributeValueExpException

class ProjectVersionsOnOffJobScriptBuilder {

    private static final VERSION_URL_ANCHOR = "<VERSION_URL>"
    private static final LATEST_ANCHOR = "<LATEST>"
    private static final PROJECT_NAME = "<PROJECT_NAME>"
    private static final PARAMETER_NAME = "<PARAMETER_NAME>"

    private static final LATEST = "version_list.add(0, 'LATEST');"

    private boolean latest
    private String parameterName
    private String projectName
    private String url

    public static ProjectVersionsOnOffJobScriptBuilder newBuilder(Map params) {
        return new ProjectVersionsOnOffJobScriptBuilder([url: params.url, latest: params.withLatest,
                                                         projectName: params.projectName,
                                                         parameterName: params.parameterName])
    }

    public String build() {
        if (url == null || url.length() == 0) {
            throw new BadAttributeValueExpException("Versions url cannot be null or empty")
        }
        if (!projectName) {
            throw new BadAttributeValueExpException("Project name cannot be null or empty")
        }
        if (!parameterName) {
            throw new BadAttributeValueExpException("Parameter name cannot be null or empty")
        }

        String latestAnchorReplacement = latest ? LATEST : ""
        return JobContext.getDSLFactory().readFileFromWorkspace('scripts/version_list_onoff_job_param.groovy')
                .replace(VERSION_URL_ANCHOR, url)
                .replace(LATEST_ANCHOR, latestAnchorReplacement)
                .replace(PROJECT_NAME, projectName)
                .replace(PARAMETER_NAME, parameterName)
    }
}
