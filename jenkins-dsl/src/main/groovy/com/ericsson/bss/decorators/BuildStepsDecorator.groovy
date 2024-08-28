package com.ericsson.bss.decorators

import com.ericsson.bss.util.JobContext
import javaposse.jobdsl.dsl.Job

class BuildStepsDecorator<T extends BuildStepsDecorator> {

    protected Job job

    BuildStepsDecorator(Job job) {
        this.job = job
    }

    /**
     * Runs a shell script.
     * @param shellScript Shell script to run
     * @return
     */
    T executeShell(String shellScript) {
        job.with {
            steps {
                shell(shellScript)
            }
        }
        return this
    }

    /**
     * Runs a shell script.
     * @param shellScript Job name
     * @return
     */
    T executeShellForJobName(String jobName) {
        job.with {
            steps {
                shell(JobContext.getDSLFactory().readFileFromWorkspace('scripts/washingmachine/' + jobName + '.sh'))
            }
        }
        return this
    }

    /**
     * Copies artifacts from another project.
     * @param jobName Name of source project.
     * @param includeGlob Relative paths to artifact(s) to copy or leave blank to copy all artifacts.
     * @param _fingerprintArtifacts Automatically fingerprints all artifacts that are copied.
     * @return
     */
    T copyArtifactsFromAnotherProject(String jobName, String includeGlob, boolean _fingerprintArtifacts = true) {
        job.with {
            steps {
                copyArtifacts(jobName) {
                    includePatterns(includeGlob)
                    fingerprintArtifacts(_fingerprintArtifacts)
                }
            }
        }
        return this
    }

    /**
     * Executes a system Groovy script.
     * @param script
     * @return
     */
    T executeSystemGroovyScript(String script) {
        job.with {
            steps {
                systemGroovyCommand(script)
            }
        }
        return this
    }

    /**
     * Triggers new parametrized builds with its current build parameters.
     * @param projects      Coma separated projects to build.
     * @param waitForJob    Blocks until the triggered projects finish their builds.
     * @param blockBuildStepFailure Fails the build step if the triggered build is worse or equal to
     *                              the threshold. Must be of 'SUCCESS', 'UNSTABLE', 'FAILURE' or 'never'.
     * @param blockFailure  Marks this build as failure if the triggered build is worse or equal to
     *                      the threshold. Must be of 'SUCCESS', 'UNSTABLE', 'FAILURE' or 'never'.
     * @param blockUnstable Mark this build as unstable if the triggered build is worse or equal to
     *                      the threshold. Must be of 'SUCCESS', 'UNSTABLE', 'FAILURE' or 'never'.
     * @return
     */
    T triggerCallBuildsOnOtherProjectsWithCurrentBuildParameters(String projects,
                                                                 boolean waitForJob = true,
                                                                 String blockBuildStepFailure = 'FAILURE',
                                                                 String blockFailure = 'FAILURE',
                                                                 String blockUnstable = 'UNSTABLE') {
        job.with {
            steps {
                downstreamParameterized {
                    trigger(projects.tokenize(',')) {
                        if (waitForJob) {
                            block {
                                buildStepFailure(blockBuildStepFailure)
                                failure(blockFailure)
                                unstable(blockUnstable)
                            }
                        }
                        parameters {
                            currentBuild()
                        }
                    }
                }
            }
        }
        return this
    }

    /**
     * Triggers new parametrized builds with properties.
     * @param projects Coma separated projects to build.
     * @param file file with properties
     * @return
     */
    T triggerCallBuildsOnOtherProjectsWithParametersFromFile(String projects, String file) {
        job.with {
            steps {
                downstreamParameterized {
                    trigger(projects.tokenize(',')) {
                        parameters {
                            propertiesFile(file)
                        }
                    }
                }
            }
        }
        return this
    }
}
