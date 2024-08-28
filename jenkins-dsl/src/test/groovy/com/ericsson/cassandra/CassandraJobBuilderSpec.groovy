
package com.ericsson.cassandra

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import spock.lang.Specification

import com.ericsson.AbstractCassandraJob;
import com.ericsson.JobSpecMixin

@Mixin(JobSpecMixin)
class CassandraJobBuilderSpec extends Specification {

    JobParent jobParent = createJobParent()

    void 'test XML output'() {
        given:
        CassandraJobBuilder builder = new CassandraJobBuilder(
                name: 'test-job',
                description: 'Build and deploy from branch testBranch',
                gitUrl: 'ssh://gerritmirror.lmera.ericsson.se:29418/cassandra/cassandra.git',
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
            triggers.'hudson.triggers.SCMTrigger'.spec.text().contains('H/30 * * * *')
            scm.userRemoteConfigs.'hudson.plugins.git.UserRemoteConfig'.url.text() == builder.gitUrl
        }
    }
}
