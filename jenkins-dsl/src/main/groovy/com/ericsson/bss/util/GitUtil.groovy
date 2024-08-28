package com.ericsson.bss.util

public class GitUtil {

    public static final String GERRIT_CENRAL_SERVER_MIRROR = 'gerritmirror.lmera.ericsson.se'
    public static final String GERRIT_CENTRAL_SERVER = 'gerrit.ericsson.se'

    private GitUtil() {
    }

    public static boolean isLocatedInGitolite(String reposiotry) {
        return reposiotry.contains('gitolite')
    }

    public static boolean containsWildcards(String branch) {
        return branch.contains('*')
    }

    public static String getCloneReference(){
        return '/workarea/bss-f_gen/kascmadm/.gitclonecache'
    }

    public static String getGitServerUrl(String server) {
        String gitServerURL
        if (server.equalsIgnoreCase('gerrit.ericsson.se')) {
            gitServerURL = "ssh://" + GERRIT_CENRAL_SERVER_MIRROR +
                    ":29418"
        }
        else {
            gitServerURL = "ssh://" + server +
                    ":29418"
        }

        return gitServerURL
    }

    public static String getGitUrl(String server, String repositoryName) {
        String gitURL
        if (isLocatedInGitolite(repositoryName)) {
            gitURL = repositoryName
        }
        else {
            gitURL = getGitServerUrl(server) + "/" + repositoryName + ".git"
        }

        return gitURL
    }
}
