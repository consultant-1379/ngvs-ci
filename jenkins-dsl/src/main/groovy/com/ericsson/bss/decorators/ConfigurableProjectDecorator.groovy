package com.ericsson.bss.decorators

import javaposse.jobdsl.dsl.Job

final class ConfigurableProjectDecorator extends ProjectDecorator<ConfigurableProjectDecorator> {

    private ConfigObject config

    ConfigurableProjectDecorator(ConfigObject config, Job job) {
        super(job)
        this.config = config
    }

    ConfigurableProjectDecorator setProjectDescriptionFromConfig() {
        String desc = config.projectDescription ?: ''
        job.with {
            description(desc)
        }
        return this
    }

    /**
     * Adds parameters to job, by parsing groovy configuration file.
     */
    ConfigurableProjectDecorator addParametersFromConfig() {
        new JobParamsDecorator(config, job).addParameters()
        return this
    }
}
