package com.ericsson.bss.job.washingmachine

class RmcaWashingMachineJobBuilder extends AbstractWashingMachineJobBuilder {

    protected static final PROJECT_TO_BUILD_AFTER_BUILD = 'rmca_washingmachine_blame'

    protected boolean hasEmailNotification() {
        return false
    }

    @Override
    protected void addSpecificConfig() {
        addBuildParametersConfig()
        addPermissionToCopyArtifact(PROJECT_TO_BUILD_AFTER_BUILD)
    }

    @Override
    protected void addProjectConfig() {
        job.with {
            wrappers {
                environmentVariables {
                    envs(injectVariables())
                    groovy(dslFactory.readFileFromWorkspace('scripts/InjectPortAllocation.groovy'))
                }
            }
        }
        super.addProjectConfig()
    }

    @Override
    protected void addScriptBuildStep() {
        job.with {
            steps {
                shell(symlinkMesosWorkSpace())
                shell(dslFactory.readFileFromWorkspace(getBuildScriptName()).replaceAll('killtimeout', DEFAULT_TAPAS_TIMEOUT.toString()))
            }
        }
    }

    @Override
    protected void addPostBuildScripts() {
        job.with {
            publishers {
                postBuildScripts {
                    steps {
                        systemGroovyCommand(dslFactory.readFileFromWorkspace('scripts/washingmachine/wm_blame_status.groovy'))
                        environmentVariables {
                            propertiesFile('env.properties')
                        }
                    }
                    onlyIfBuildSucceeds(false)
                }
            }
        }
    }

    @Override
    protected void addArchiveArtifactsConfig() {
        job.with {
            publishers {
                archiveArtifacts {
                    pattern('rmcarpms/*.rpm')
                    pattern('*/jive/*.jar')
                    pattern('selenium_screenshoots.tar.gz')
                    allowEmpty(true)
                    onlyIfSuccessful(false)
                    fingerprint(false)
                    defaultExcludes(true)
                }
            }
        }
    }

    @Override
    protected void addTriggerParameterizedBuildOnOtherProjectsConfig() {
        job.with {
            publishers {
                downstreamParameterized {
                    trigger(PROJECT_TO_BUILD_AFTER_BUILD) {
                        condition('FAILED_OR_BETTER')
                        parameters {
                            predefinedProp('JENKINS_URL', '$BUILD_URL')
                            predefinedProp('TAPAS_URL', '$tapas_web_url')
                            predefinedProp('JIVE_URL', '$jive_web_url')
                            predefinedProp('UPSTREAM_JOB', '$JOB_NAME')
                            predefinedProp('BLAME_CONFIG_FILE', '/proj/eta-automation/tapas/config/rmca/config/blame/general-blame-config.json,' +
                                '/proj/eta-automation/blame_mail/wm-blame-config-rmca.json')
                            predefinedProp('STATUS', '$WM_BLAME_STATUS')
                            predefinedProp('CISCAT_RESULT', '$CISCAT_RESULT')
                            predefinedProp('DEFAULT_RECIPIENTS', 'rmca_washingmachine@mailman.lmera.ericsson.se')
                        }
                    }
                }
            }
        }
    }

    protected void addBuildParametersConfig() {
        job.with {
            parameters {
                choiceParam('HOST_SET', ['alternate', '1', '2'],
                        'alternate: Alternate between 1 and 2 automatically<br/>\n' +
                                '1: RMCA=vma-rmca0003, MSV=vma-rmca0001, CIL=vma-rmca0002<br/>\n' +
                                '2: RMCA=vma-rmca0006, MSV=vma-rmca0004, CIL=vma-rmca0005<br/>\n' +
                                'InstallNode is always vmx-rmca150')
            }
        }
    }

    protected Map injectVariables() {
        def env = [:]
        env.put("WORKSPACE_ORIG", "\${WORKSPACE}")
        env.put("WORKSPACE", getWorkspaceLocation())
        env.put("WS_TMP", getWorkspaceLocation() + "/.tmp")
        env.put('GIT_HOME', '/opt/local/dev_tools/git/latest')
        env.put('MAVEN_REPOSITORY', '${WORKSPACE}/.repository')
        env.put('GIT_CLONE_CACHE', '/workarea/bss-f_gen/kascmadm/.gitclonecache')
        env.put('MAVEN_SETTINGS', MAVEN_SETTINGS_PATH + 'kascmadm-settings_arm-rmca.xml')
        env.put('JAVA_TOOL_OPTIONS', '-Xms128M -Xmx3G -XX:MaxPermSize=256m -XX:SelfDestructTimer=45 -Djava.io.tmpdir=${WS_TMP} ' +
                '-DOSGI_PORT_OVERRIDE=\${ALLOCATED_PORT}')
        env.put('M2_HOME', '/opt/local/dev_tools/maven/apache-maven-3.2.1')
        env.put('M2', '${M2_HOME}/bin')
        env.put('MAVEN_OPTS', '-Xms128M -Xmx3G -XX:MaxPermSize=256m -XX:SelfDestructTimer=45 -Djava.io.tmpdir=${WS_TMP} ' +
                '-DOSGI_PORT_OVERRIDE=\${ALLOCATED_PORT}')
        env.put('CDT_INSTALL_FOLDER', getWorkspaceLocation())
        env.put('FIREFOXDIR', '/proj/eta-tools/firefox/45.0esr/Linux_i386_64/firefox/firefox')
        env.put('npm_config_cache', '${WS_TMP}/npm_config_cache')
        env.put('npm_config_prefix', '${CDT_INSTALL_FOLDER}/node_modules')
        env.put('PATH', '${GIT_HOME}/bin:${M2}:${FIREFOXDIR}:${npm_config_prefix}/bin/:/opt/local/dev_tools/nodejs/node-v0.10.20-linux-x64/bin/:${PATH}')
        return env
    }

}
