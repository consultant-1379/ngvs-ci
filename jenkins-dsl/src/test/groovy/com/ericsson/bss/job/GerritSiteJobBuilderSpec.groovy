package com.ericsson.bss.job

import com.ericsson.JobSpecMixin
import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import spock.lang.Specification

@Mixin(JobSpecMixin)
class GerritSiteJobBuilderSpec extends Specification {

    JobParent jobParent = createJobParent()

    void 'test default gerrit filepath trigger'() {
        String onlySiteSourceFolder= '.*src\\/site.*'

        given:

        GerritSiteJobBuilder builder = getDefaultGerritSiteObject(onlySiteSourceFolder)

        when:
        Job job = builder.build()

        then:
        builder.jobName != null

        with(job.node) {
            triggers.'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger'.
                    gerritProjects.'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject'.filePaths.
            'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.FilePath'.pattern.text() == onlySiteSourceFolder
        }
    }

    void 'test should trigger on specific folder'() {
    String specificFolder = 'path/to/specific/folder'

        given:

        GerritSiteJobBuilder builder = getDefaultGerritSiteObject(specificFolder)

        when:
        Job job = builder.build()

        then:
        builder.jobName != null

        with(job.node) {
            triggers.'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger'.
                    gerritProjects.'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject'.filePaths.
            'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.FilePath'.pattern.text() == specificFolder
        }
    }

    void 'test should trigger for everything'() {

        given:

        GerritSiteJobBuilder builder = getDefaultGerritSiteObject('.*')

        when:
        Job job = builder.build()

        then:
        builder.jobName != null

        with(job.node) {
            triggers.'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger'.
                    gerritProjects.'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject'.filePaths.text() == ''
        }
    }

    private GerritSiteJobBuilder getDefaultGerritSiteObject(String triggerFilePath) {
        return new GerritSiteJobBuilder(
                gerritUser: 'gerritUser',
                gerritServer: 'gerritServer',
                gerritName: 'gerritName',
                jobName: 'test-job',
                projectName: 'projectName',
                triggerFilePath: triggerFilePath,
                dslFactory: jobParent
        )
    }
}
