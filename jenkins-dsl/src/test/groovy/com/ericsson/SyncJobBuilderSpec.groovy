package com.ericsson


import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import spock.lang.Specification

@Mixin(JobSpecMixin)
class SyncJobBuilderSpec extends Specification {

    JobParent jobParent = createJobParent()

    void 'test XML output'() {
        given:
        SyncJobBuilder builder = new SyncJobBuilder(
                name: 'test-job',
                description: 'Build and deploy from branch testBranch',
                branch: 'origin/testBranch',
                workspace: 'workspace'
                )

        when:
        Job job = builder.build(jobParent)

        then:
        job.name == builder.name
        with(job.node) {
            name() == 'project'
            description.text() == builder.description + AbstractCassandraJob.CASSANDRA_TROUBLESHOOT
            triggers.'hudson.triggers.TimerTrigger'.spec.text() == 'H */2 * * *'
            logRotator.daysToKeep.text().toInteger() == 10
            logRotator.numToKeep.text().toInteger() == 10
            logRotator.artifactDaysToKeep.text().toInteger() == 5
            logRotator.artifactNumToKeep.text().toInteger() == 5
        }
    }
}
