package com.ericsson.bss.job

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import spock.lang.Specification

import com.ericsson.JobSpecMixin

@Mixin(JobSpecMixin)
class GerritIntegrationTestJobBuilderSpec extends Specification {

    JobParent jobParent = createJobParent()

    void 'test XML output'() {
        given:
        GerritIntegrationTestJobBuilder builder = new GerritIntegrationTestJobBuilder(
                workspacePath: '/job/location/',
                gerritUser: 'gerrit-user',
                gerritServer: 'gerrit-server',
                jobName: 'test-job',
                mavenSettingsFile: '/path/to/settings.xml',
                gerritName: 'gerrit-repository-name',
                projectName: 'project name',
                integrationTestRepository: 'integration_test_repository',
                dslFactory: jobParent,
                //Should try to inject the script location
                junitWorkaroundScriptFile: '',
                injectPortAllocation: ""
                )

        when:
        Job job = builder.build()

        then:
        builder.jobName != null
        new XmlNodePrinter(preserveWhitespace:true).print(job.node)

        with(job.node) {
        }
    }
}
