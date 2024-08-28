package com.ericsson.bss.decorators

import javaposse.jobdsl.dsl.Job

class BuildTriggersDecorator<T extends BuildTriggersDecorator> {

    protected Job job

    BuildTriggersDecorator(Job job) {
        this.job = job
    }

    /**
     * Triggers the job based on regular intervals.
     * @param cronExpression
     * @return
     */
    T buildPeriodically(String cronExpression) {
        job.with {
            triggers {
                cron(cronExpression)
            }
        }
        return this
    }

    /**
     * Starts a build on completion of an upstream job, i.e. adds the "Build after other projects are built" trigger.
     * @param project upstream job
     * @param threshold Possible thresholds are 'SUCCESS', 'UNSTABLE' or 'FAILURE'.
     * @return
     */
    T buildAfterOtherProjectsAreBuilt(String project, String threshold = 'SUCCESS') {
        job.with {
            triggers {
                upstream(project, threshold)
            }
        }
        return this
    }
}
