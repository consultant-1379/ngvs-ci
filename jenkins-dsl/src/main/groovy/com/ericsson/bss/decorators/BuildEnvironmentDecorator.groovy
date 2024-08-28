package com.ericsson.bss.decorators

import javaposse.jobdsl.dsl.Job

class BuildEnvironmentDecorator<T extends BuildEnvironmentDecorator> {

    protected static final DEFAULT_ABORT_DESC = 'Build aborted due to timeout after {0} minutes'
    protected static final SCREEN_RESOLUTION_PIXEL_DEPTH = '1280x1024x24'
    protected static final XVFB_TIMEOUT = 3

    protected Job job

    BuildEnvironmentDecorator(Job job) {
        this.job = job
    }

    /**
     * Deletes files from the workspace before the build starts.
     * @return
     */
    T deleteWorkspaceBeforeBuildStarts() {
        job.with {
            wrappers {
                preBuildCleanup()
            }
        }
        return this
    }

    /**
     * Adds a number of environment variables with information about the current user.
     * @return
     */
    T setJenkinsUserBuildVariables() {
        job.with {
            wrappers {
                buildUserVars()
            }
        }
        return this
    }

    /**
     * Abort the build after specified timeout time.
     * @param timeoutMinutes
     */
    T abortTheBuildIfItStuck(int timeoutMinutes, String description = DEFAULT_ABORT_DESC) {
        job.with {
            wrappers {
                timeout {
                    absolute(timeoutMinutes)
                    abortBuild()
                    writeDescription(description)
                }
            }
        }
        return this
    }

    /**
     * Adds environment variables to the build.
     * @param variables
     * @return
     */
    T injectEnvironmentVariablesToTheBuildProcess(Map variables, String groovyScripts) {
        job.with {
            wrappers {
                environmentVariables {
                    envs(variables)
                    groovy(groovyScripts)
                }
            }
        }
        return this
    }

    /**
     * Fail the build after specified timeout time.
     * @param timeoutMinutes
     */
    T failTheBuildIfItStuck(int timeoutMinutes, String description = DEFAULT_ABORT_DESC) {
        job.with {
            wrappers {
                timeout {
                    absolute(timeoutMinutes)
                    failBuild()
                    writeDescription(description)
                }
            }
        }
        return this
    }

    /**
     * Run a Xvnc session during a build.
     * @param _takeScreenshot Takes a screenshot upon completion of the build.
     * @param _useXauthority Creates a dedicated Xauthority file per build.
     * @return
     */
    T runXvncDuringBuild(boolean _takeScreenshot = false,
                                                 boolean _useXauthority = true) {
        job.with {
            wrappers {
                xvnc {
                    takeScreenshot(_takeScreenshot)
                    useXauthority(_useXauthority)
                }
            }
        }
        return this
    }

    /**
     * Run a Xvfb session during a build.
     * @param _autoDisplayName Lets Xvfb choose the display number automatically.
     * @param _screenResolutionAndPixelDepth Changes the screen resolution and pixel depth.
     * @param _xvfbTimeOut Specifies the number of seconds to wait for Xvfb to start.
     * @return
     */
    T runXvfbDuringBuild(boolean _autoDisplayName = true,
            String _screenResolutionAndPixelDepth = SCREEN_RESOLUTION_PIXEL_DEPTH,
            int _xvfbTimeOut = XVFB_TIMEOUT,
            int _displayNameOffset = 1,
            boolean _shutdownWithBuild = true,
            boolean _parallelBuild = true) {
        job.with {
            wrappers {
                xvfb('default') {
                    autoDisplayName(_autoDisplayName)
                    displayNameOffset(_displayNameOffset)
                    parallelBuild(_parallelBuild)
                    screen(_screenResolutionAndPixelDepth)
                    shutdownWithBuild(_shutdownWithBuild)
                    timeout(_xvfbTimeOut)
                }
            }
        }
        return this
    }

    /**
     * Adds timestamps to the console log.
     * @return
     */
    T addTimestampsToTheConsoleOutput() {
        job.with {
            wrappers {
                timestamps()
            }
        }
        return this
    }
}
