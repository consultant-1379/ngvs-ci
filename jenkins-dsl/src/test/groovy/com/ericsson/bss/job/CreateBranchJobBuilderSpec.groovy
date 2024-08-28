package com.ericsson.bss.job

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import spock.lang.Specification

import com.ericsson.JobSpecMixin

@Mixin(JobSpecMixin)
class CreateBranchJobBuilderSpec extends Specification {

    JobParent jobParent = createJobParent()

    void 'test XML output'() {
        given:
        CreateBranchJobBuilder builder = new CreateBranchJobBuilder(
                workspacePath: '/job/location/',
                gerritUser: 'gerrit-user',
                gerritServer: 'gerrit-server',
                jobName: 'test-job',
                mavenSettingsFile: '/path/to/settings.xml',
                gerritName: 'gerrit-repository-name',
                dslFactory: jobParent,
                //Should try to inject the script location
                createBranchScriptFile: "",
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
