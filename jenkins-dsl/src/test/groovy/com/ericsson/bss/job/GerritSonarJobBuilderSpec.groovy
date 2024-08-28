package com.ericsson.bss.job

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import spock.lang.Specification

import com.ericsson.JobSpecMixin

@Mixin(JobSpecMixin)
class GerritSonarJobBuilderSpec extends Specification {

    JobParent jobParent = createJobParent()

    void 'test XML output'() {
        given:
        GerritSonarJobBuilder builder = new GerritSonarJobBuilder(
                workspacePath: '/job/location/',
                gerritUser: 'gerrit-user',
                gerritServer: 'gerrit-server',
                jobName: 'test-job',
                mavenSettingsFile: '/path/to/settings.xml',
                verifyCommitMessage: "",
                gerritName: 'gerrit-repository-name',
                dslFactory: jobParent,
                injectPortAllocation: ""
                )

        when:
        Job job = builder.build()

        then:
        builder.jobName != null
        new XmlNodePrinter(preserveWhitespace:true).print(job.node)
    }
}
