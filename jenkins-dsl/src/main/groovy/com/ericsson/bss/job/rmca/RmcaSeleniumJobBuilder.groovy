package com.ericsson.bss.job.rmca

import javaposse.jobdsl.dsl.Job

import com.ericsson.bss.AbstractJobBuilder

class RmcaSeleniumJobBuilder extends AbstractJobBuilder {

    private String gerritName

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        timeoutForJob = 45
        addSeleniumConfig()
        return job
    }

    private void addSeleniumConfig() {
        RmcaSeleniumHelper helper = new RmcaSeleniumHelper(this)
        helper.addCommonSeleniumConfig(profilesToBeUsed)
        helper.addKillBackendCommand()
        job.with {
            addGitRepository(gerritName)
            triggers { scm('H/30 * * * *\n# Realtime pushed by the eta_gitscmpoll_trigger job') }
            steps {
                if (symlinkWorkspace) {
                    shell(symlinkMesosWorkSpace())
                }
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
                shell(removeOldArtifacts())
                shell(gitConfig("\${WORKSPACE}"))
                shell(helper.getBuildCommand())
                shell(helper.getRunBackendCommand())
                shell(helper.getSeleniumCommand(profilesToBeUsed))
            }
            publishers {
                archiveJunit('**/target/reports/junit/')
                configure { project ->
                    project / publishers << 'net.masterthought.jenkins.CucumberReportPublisher' {
                        jsonReportDirectory 'selenium/target/reports/'
                        pluginUrlPath '/jenkins/job/rmca/job/rmca/'
                        fileIncludePattern ''
                        fileExcludePattern ''
                        skippedFails false
                        pendingFails false
                        undefinedFails false
                        missingFails false
                        noFlashCharts false
                        ignoreFailedTests true
                        parallelTesting false
                    }
                }
                wsCleanup()
            }
        }
    }
}
