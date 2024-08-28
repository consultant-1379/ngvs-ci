package com.ericsson.bss.job

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import spock.lang.Specification

import com.ericsson.JobSpecMixin

@Mixin(JobSpecMixin)
class DeployJobBuilderSpec extends Specification {

    JobParent jobParent = createJobParent()

    void 'test XML output'() {
        given:
        DeployJobBuilder builder = new DeployJobBuilder(
                workspacePath: '/job/location/',
                gerritUser: 'gerrit-user',
                gerritServer: 'gerrit-server',
                releaseGoal: 'release goal',
                releaseDryrunGoal: 'release dryrun goal',
                jobName: 'test-job',
                mavenSettingsFile: '/path/to/settings.xml',
                gerritName: 'gerrit name',
                projectName: 'project name',
                dslFactory: jobParent,
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
