package com.ericsson.bss.job

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import spock.lang.Specification
import com.ericsson.JobSpecMixin

import com.ericsson.bss.job.washingmachine.WashingMachineUpgradeOnOffJobBuilder

@Mixin(JobSpecMixin)
class WashingmachineUpgradeOnOffBuilderSpec extends Specification {

    JobParent jobParent = createJobParent()
    String projectName = "eta"
    String suffix = "_upgrade"
    String recipient = "none@ericsson.com"

    ArrayList variantArtifacts =
    [
        [name:'CORE_FROM_VERSION', groupid:'com.ericsson.bss.rm', artifactid:'charging.core',
            types: ['release', 'dev'], desc: "Version of a base image too upgrade from."],
        [name:'CORE_TO_VERSION', groupid:'com.ericsson.bss.rm', artifactid:'charging.core',
            types: ['release', 'dev'], desc: "Version of a base image too upgrade to."],
        [name:'ACCESS_FROM_VERSION', groupid:'com.ericsson.bss.rm', artifactid:'charging.access',
            types: ['release', 'dev'], desc: "Version of a base image too upgrade from."],
        [name:'ACCESS_TO_VERSION', groupid:'com.ericsson.bss.rm', artifactid:'charging.access',
            types: ['release', 'dev'], desc: "Version of a base image too upgrade to."],
        [name:'DLB_FROM_VERSION', groupid:'com.ericsson.bss.rm', artifactid:'charging.dlb',
            types: ['release', 'dev'], desc: "Version of a base image too upgrade from."],
        [name:'DLB_TO_VERSION', groupid:'com.ericsson.bss.rm', artifactid:'charging.dlb',
            types: ['release', 'dev'], desc: "Version of a base image too upgrade to."]
    ]

    void 'test XML output'() {
        given:
        WashingMachineUpgradeOnOffJobBuilder builder = WashingMachineUpgradeOnOffJobBuilder.newInstance(
            gerritUser       : 'gerrit-user',
            gerritServer     : 'gerrit-server',
            dslFactory       : jobParent,
            projectName      : projectName,
            suffix           : suffix,
            recipient        : recipient,
            variantArtifacts : variantArtifacts
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
