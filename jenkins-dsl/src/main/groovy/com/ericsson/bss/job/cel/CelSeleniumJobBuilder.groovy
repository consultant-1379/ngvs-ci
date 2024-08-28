package com.ericsson.bss.job.cel

import javaposse.jobdsl.dsl.Job

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.job.cel.CelSeleniumHelper

class CelSeleniumJobBuilder extends AbstractJobBuilder {

    private String gerritName

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        timeoutForJob = 45
        addSeleniumConfig()
        return job
    }

    private void addSeleniumConfig() {
        CelSeleniumHelper helper = new CelSeleniumHelper(this)
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
                        jsonReportDirectory 'ui/selenium/target/reports/'
                        pluginUrlPath '/jenkins/job/cel/job/cel/'
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
            }
        }
    }
}
