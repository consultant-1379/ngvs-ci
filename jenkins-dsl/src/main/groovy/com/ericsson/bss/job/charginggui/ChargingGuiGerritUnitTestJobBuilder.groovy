package com.ericsson.bss.job.charginggui

import com.ericsson.bss.job.MvnGerritUnitTestJobBuilder

class ChargingGuiGerritUnitTestJobBuilder extends MvnGerritUnitTestJobBuilder {
    @Override
    protected String mavenBuildCommand(){
        String cmd = getShellCommentDescription("Limit memory, must in the same shell where test running.") +
                    "# Sanity limit memory to 16Gb to protect from gconfd-2 memleak bug\n" +
                    "ulimit -v 16000000\n" +
                    "ulimit -a\n" +
                    "cd \${WORKSPACE}\n" +
                    getShellCommentDescription("Maven build command") +
                    "mvn \\\n" +
                    "clean install \\\n" +
                    "-Dfc.local=true -PincludeFunctionControlProvider \\\n" +
                    "-Dsurefire.useFile=false \\\n" +
                    getMavenGeneralBuildParameters() + "\n" +
                    getShellCommentDescription("Start selenidetest backend") +
                    dslFactory.readFileFromWorkspace("scripts/charging_gui_selenidetest_backend.sh") +
                    "mvn \\\n" +
                    "clean test \\\n" +
                    "-f \${WORKSPACE}/selenidetest/pom.xml \\\n" +
                    "-DskipTests=false \\\n" +
                    "-Dskip.selenide=false \\\n" +
                    "-Dsurefire.useFile=false \\\n" +
                    getMavenGeneralBuildParameters()
        return cmd
    }
    @Override
    protected void setPublishers() {
        job.with {
            publishers {
                archiveArtifacts {
                    pattern('selenidetest/build/reports/tests/*.png')
                    allowEmpty(true)
                }
            }
        }
        super.setPublishers()
    }
}
