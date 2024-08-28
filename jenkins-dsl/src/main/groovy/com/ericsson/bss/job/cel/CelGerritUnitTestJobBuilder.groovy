package com.ericsson.bss.job.cel

import com.ericsson.bss.job.MvnGerritUnitTestJobBuilder

public class CelGerritUnitTestJobBuilder extends MvnGerritUnitTestJobBuilder {

    @Override
    protected void addUnitTestConfig() {
        super.addUnitTestConfig()
        job.with {
            addTimeoutAndAbortConfig(45)
        }
    }

    protected String mavenBuildCommand() {
        String cmd = getShellCommentDescription("Limit memory, must in the same shell where test running.") +
                "# Sanity limit memory to 16Gb to protect from gconfd-2 memleak bug\n" +
                "ulimit -v 16000000\n" +
                "ulimit -a\n" +
                "cd \${WORKSPACE}\n" +
                getShellCommentDescription("Maven build command") +
                "mvn \\\n" +
                "clean install \\\n"

        cmd = addExtraMavenFlags(cmd) + "\n"

        return cmd
    }

    private addExtraMavenFlags(String cmd) {
        if (runParallelThreads) {
            cmd += "-DparallelTests -T 4 \\\n"
        }
        cmd += "-Dsurefire.useFile=false \\\n" +
                getMavenGeneralBuildParameters()

        return cmd
    }
}
