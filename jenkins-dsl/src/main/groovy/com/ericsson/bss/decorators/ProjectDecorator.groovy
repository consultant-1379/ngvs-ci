package com.ericsson.bss.decorators

import javaposse.jobdsl.dsl.Job

class ProjectDecorator<T extends ProjectDecorator> {

    protected static final JDK_VERSION = 'Latest JDK 1.7 64bit'
    protected static final RESTRICT_LABEL_EXECUTOR = 'Linux_redhat_6.2_x86_64_mesos'

    protected Job job

    ProjectDecorator(Job job) {
        this.job = job
    }

    T setProjectDescription(String desc) {
        job.with {
            description(desc)
        }
        return this
    }

    T discardOldBuilds(Integer daysToKeepBuilds, Integer maxBuildsToKeep) {
        job.with {
            logRotator(daysToKeepBuilds, maxBuildsToKeep)
        }
        return this
    }

    /**
     * Block build if certain job is running.
     * @param projectName Name of the certain job. Regular expressions can be used for the project names, e.g. /.*-maintenance/ will match all maintenance jobs.
     * @param blockLevel Possible values are 'GLOBAL' and 'NODE' (default).
     * @param scanQueueFor Possible values are 'ALL', 'BUILDABLE' and 'DISABLED' (default).
     */
    T blockBuildIfJobsAreRunning(String projectName, String _blockLevel, String _scanQueueFor) {
        job.with {
            blockOn(projectName) {
                blockLevel(_blockLevel)
                scanQueueFor(_scanQueueFor)
            }
        }
        return this
    }

    /**
     * Allows to configure projects which can copy artifacts of this project.
     * @param allowedProjects Comma seperated list of projects that can copy artifacts of this project. Wild card character ('*') is available.
     */
    T permissionToCopyArtifact(String allowedProjects) {
        job.with {
            configure {
                it / 'properties' / 'hudson.plugins.copyartifact.CopyArtifactPermissionProperty' / 'projectNameList' {
                    string(allowedProjects)
                }
            }
        }
        return this
    }

    /**
     * Allows to configure projects which can copy artifacts of this project.
     * @param allowedProjects List of projects that can copy artifacts of this project. Wild card character ('*') is available.
     */
    T permissionToCopyArtifact(List<String> allowedProjects) {
        addPermissionToCopyArtifact(allowedProjects.join(','))
        return this
    }

    /**
     * Name of the JDK installation to use for this job. The name must match the name of a JDK installation defined in the Jenkins system configuration.
     * The default JDK will be used when the jdk method is omitted.
     * @param version
     * @return
     */
    T jdk(String version = JDK_VERSION) {
        job.with {
            jdk(version)
        }
        return this
    }

    /**
     * Label which specifies which nodes this job can run on. If null is passed in, the label is cleared out and the job can roam.
     * @param labelExpression
     * @return
     */
    T restrictWhereThisProjectCanBeRun(String labelExpression = RESTRICT_LABEL_EXECUTOR) {
        job.with {
            label(labelExpression)
        }
        return this
    }

    /**
     * Allows Jenkins to schedule and execute multiple builds concurrently.
     * @param _concurrentBuild
     * @return
     */
    T executeConcurrentBuildsIfNecessary(boolean _concurrentBuild = true) {
        job.with {
            concurrentBuild(_concurrentBuild)
        }
        return this
    }

    /**
     * Block build if certain jobs are running.
     * @param projects Coma separated project names to wait for.
     * @param _blockLevel Possible values are 'GLOBAL' (default) and 'NODE'.
     * @param _scanQueueFor Possible values are 'ALL' (default), 'BUILDABLE' and 'DISABLED'.
     * @return
     */
    T blockBuildIfCertainJobsAreRunning(String projects,
                                        String _blockLevel='GLOBAL',
                                        String _scanQueueFor='ALL') {
        job.with {
            blockOn(projects.tokenize(',')) {
                blockLevel(_blockLevel)
                scanQueueFor(_scanQueueFor)
            }
        }
        return this
    }

    /**
     * Defines a time to wait before triggering a build
     * @param seconds Time to wait
     * @return
     */
    T quietPeriod(int seconds) {
        job.with {
            quietPeriod(seconds)
        }
        return this
    }
}
