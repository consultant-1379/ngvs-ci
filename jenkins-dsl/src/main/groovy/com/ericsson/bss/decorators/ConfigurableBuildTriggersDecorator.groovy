package com.ericsson.bss.decorators

import javaposse.jobdsl.dsl.Job

final class ConfigurableBuildTriggersDecorator extends BuildTriggersDecorator<ConfigurableBuildTriggersDecorator> {

    private ConfigObject config

    ConfigurableBuildTriggersDecorator(ConfigObject config, Job job) {
        super(job)
        this.config = config
    }

    ConfigurableBuildTriggersDecorator buildPeriodicallyFromConfig() {
        job.with {
            triggers {
                cron(config.cronExpression)
            }
        }
        return this
    }
}
