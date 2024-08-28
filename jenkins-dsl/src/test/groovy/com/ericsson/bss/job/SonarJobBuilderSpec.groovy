package com.ericsson.bss.job

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import spock.lang.Specification

import com.ericsson.JobSpecMixin

@Mixin(JobSpecMixin)
class SonarJobBuilderSpec extends Specification {

    JobParent jobParent = createJobParent()

    void 'test XML output'() {
        given:
        SonarJobBuilder builder = new SonarJobBuilder(
                gerritUser: 'gerrit-user',
                gerritServer: 'gerrit-server',
                jobName: 'test-job',
                mavenSettingsFile: '/path/to/settings.xml',
                gerritName: 'gerrit name',
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
