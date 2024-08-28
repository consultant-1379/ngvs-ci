package com.ericsson.bss

public abstract class AbstractGerritTestJobBuilder extends AbstractGerritJobBuilder {

    protected String junitWorkaroundScriptFile = 'scripts/junit_workaround.sh'

    protected String junitPublisherWorkaround() {
        if (junitWorkaroundScriptFile.equals("")) {
            return ""
        }
        else {
            return dslFactory.readFileFromWorkspace(junitWorkaroundScriptFile)
        }
    }
}
