package com.ericsson.bss.decorators.scriptbuilders

import com.ericsson.bss.util.JobContext

import javax.management.BadAttributeValueExpException

class ProjectVersionsScriptBuilder {

    private static final VERSION_URL_ANCHOR = "<VERSION_URL>"
    private static final LATEST_ANCHOR = "<LATEST>"

    private static final LATEST = "version_list.add(0, 'LATEST');"

    private String url
    private boolean latest

    public static ProjectVersionsScriptBuilder newBuilder(Map params) {
        return new ProjectVersionsScriptBuilder([url: params.url, latest: params.withLatest])
    }

    public String build() {
        if (url == null || url.length() == 0) {
            throw new BadAttributeValueExpException("Versions url cannot be null or empty")
        }

        String latestAnchorReplacement = latest ? LATEST : ""
        return JobContext.getDSLFactory().readFileFromWorkspace('scripts/version_list_param.groovy')
                .replace(VERSION_URL_ANCHOR, url)
                .replace(LATEST_ANCHOR, latestAnchorReplacement)
    }
}
