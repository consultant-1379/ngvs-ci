package com.ericsson.javadriver

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import spock.lang.Specification

import com.ericsson.JobSpecMixin

@Mixin(JobSpecMixin)
class JavaDriverCreateEricssonBranchJobBuilderSpec extends Specification {

    JobParent jobParent = createJobParent()

    void 'test XML output'() {
        given:
        JavaDriverCreateEricssonBranchJobBuilder builder = new JavaDriverCreateEricssonBranchJobBuilder(
                name: 'test-job',
                gitUrl: 'ssh://gerritmirror.lmera.ericsson.se:29418/cassandra/java-driver.git',
                workspace: 'workspace',
                createBranchScriptFile: ''
                )

        when:
        Job job = builder.build(jobParent)

        then:
        new XmlNodePrinter(preserveWhitespace:true).print(job.node)

        with(job.node) {
        }
    }
}
