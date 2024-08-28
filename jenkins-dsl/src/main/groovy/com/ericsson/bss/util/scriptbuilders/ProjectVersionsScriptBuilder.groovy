package com.ericsson.bss.util.scriptbuilders

import javax.management.BadAttributeValueExpException

class ProjectVersionsScriptBuilder {

    private static final VERSION_URL_ANCHOR = "<VERSION_URL>"
    private static final LATEST_ANCHOR = "<LATEST>"

    private static final LATEST = "version_list.add(0, 'LATEST');"

    def dslFactory
    private String url
    private boolean latest

    public static ProjectVersionsScriptBuilder newBuilder(def dslFactory) {
        return new ProjectVersionsScriptBuilder([dslFactory: dslFactory, latest:false])
    }

    public String build() {
        if (url == null || url.length() == 0) {
            throw new BadAttributeValueExpException("Versions url cannot be null or empty")
        }
        String latestAnchorReplacement = latest ? LATEST : ""
        return dslFactory.readFileFromWorkspace('scripts/version_list_param.groovy')
                .replace(VERSION_URL_ANCHOR, url)
                .replace(LATEST_ANCHOR, latestAnchorReplacement)
    }

    /**
     * Fetches versions of the project from the given url address
     * @param url Url link to file with versions of the project
     * @return
     */
    public ProjectVersionsScriptBuilder withUrl(String url) {
        this.url = url
        return this
    }

    /**
     * Adds 'LATEST' to versions of the project at 0 index
     * @return
     */
    public ProjectVersionsScriptBuilder withLatestVersion() {
        this.latest = true
        return this
    }
}
