package com.ericsson.bss

import com.ericsson.bss.decorators.ConfigurableBuildTriggersDecorator
import com.ericsson.bss.decorators.ConfigurablePostBuildStepsDecorator
import com.ericsson.bss.decorators.ConfigurableProjectDecorator

/**
 * Job which uses groovy configuration files.
 */
public abstract class AbstractConfigurableJobBuilder extends AbstractJobBuilder {

    protected ConfigObject config

    protected void init(String jobName, String baseConfigFileName) {
        super.init(jobName)
        readConfigurationFiles(baseConfigFileName)
    }

    @Override
    protected ConfigurableProjectDecorator configureProject() {
        return new ConfigurableProjectDecorator(config, job)
    }

    @Override
    protected ConfigurablePostBuildStepsDecorator configurePostBuildSteps() {
        return new ConfigurablePostBuildStepsDecorator(config, job)
    }

    @Override
    protected ConfigurableBuildTriggersDecorator configureBuildTriggers() {
        return new ConfigurableBuildTriggersDecorator(config, job)
    }

    private void readConfigurationFiles(String baseFileName) {
        ConfigSlurper cs = new ConfigSlurper()
        String commonConfiguration = 'configurations/common.groovy'
        String basicConfiguration = 'configurations/' + baseFileName + '.groovy'
        String additionalConfiguration = 'configurations/' + baseFileName + '_' + jobName + '.groovy'

        out.println("Trying to load common configuration file: " + commonConfiguration)
        config = cs.parse(dslFactory.readFileFromWorkspace(commonConfiguration))
        out.println("Common configuration file has been loaded.")

        try {
            out.println("Trying to load basic configuration file: " + basicConfiguration)
            String basicConf = dslFactory.readFileFromWorkspace(basicConfiguration)
            config.merge(cs.parse(basicConf))
            out.println("Basic configuration file has been loaded.")
        } catch (Exception e) {
            out.println("Cannot load file")
            out.println(e.getMessage())
        }

        try {
            out.println("Trying to load additional configuration file: " + additionalConfiguration)
            String additionalConf = dslFactory.readFileFromWorkspace(additionalConfiguration)
            config.merge(cs.parse(additionalConf))
            out.println("Additional configuration file has been loaded.")
        } catch (Exception e) {
            out.println("Cannot load file")
            out.println(e.getMessage())
        }

        prettyPrintConfigurationParameters()
    }

    private void prettyPrintConfigurationParameters() {
        out.println('\n----- Configuration file parameters -----')
        for (String key : config.keySet()) {
            if (config.get(key) instanceof ArrayList || config.get(key) instanceof Map) {
                out.println(key + ':')
                for (String value : config.get(key)) {
                    out.println('  ' + value)
                }
            } else {
                out.println(key + ': ' + config.get(key))
            }
        }
        out.println('-----------------------------------------\n')
    }
}
