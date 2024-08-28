package com.ericsson.bss.job.rmca

import com.ericsson.bss.job.MvnGerritUnitTestJobBuilder

public class RmcaGerritUnitTestJobBuilder extends MvnGerritUnitTestJobBuilder {

    @Override
    protected void addUnitTestConfig() {
        extraEnvironmentVariables = [:]
        extraEnvironmentVariables.put("M2_HOME", "/opt/local/dev_tools/maven/apache-maven-3.3.9")
        runParallelThreads = true

        super.addUnitTestConfig()
        job.with {
            addTimeoutAndAbortConfig(60)
        }
    }

    protected String mavenBuildCommand() {
        String cmd = getShellCommentDescription("Limit memory, must in the same shell where test running.") +
                "# Sanity limit memory to 16Gb to protect from gconfd-2 memleak bug\n" +
                "ulimit -v 16000000\n" +
                "ulimit -a\n" +
                "cd \${WORKSPACE}\n" +
                getShellCommentDescription("Maven build command") +
                "if git diff --quiet HEAD~1 compile/pom.xml pom.xml ui \n" +
                "then \n" +
                "mvn \\\n" +
                "clean install \\\n"

        cmd = addExtraMavenFlags(cmd) + "\n"

        cmd += "else \n"
        cmd += "mvn \\\n" +
               "clean install \\\n"

        if (profilesToBeUsed != null && profilesToBeUsed != "") {
            cmd += '-P' + profilesToBeUsed + ' \\\n'
        }

        cmd = addExtraMavenFlags(cmd) + "\n"

        cmd += "fi"

        return cmd
    }

    private addExtraMavenFlags(String cmd) {
        if (runParallelThreads) {
            cmd += "-DparallelTests \\\n"
        }
        cmd += "-Dsurefire.useFile=false \\\n" +
                getMavenGeneralBuildParameters()

        return cmd
    }
}
