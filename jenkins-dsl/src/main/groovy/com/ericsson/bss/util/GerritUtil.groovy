package com.ericsson.bss.util

public class GerritUtil {

    private static final int GERRIT_PORT = 29418

    private GerritUtil() {
    }

    public static String getAllProjectsCommand(String server) {
        String command = 'ssh -o BatchMode=yes -p ' + GERRIT_PORT + ' ' + server + ' gerrit ls-projects'

        return command
    }
}
