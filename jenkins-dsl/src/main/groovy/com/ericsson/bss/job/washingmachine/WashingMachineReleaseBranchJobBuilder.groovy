package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractTapasJobBuilder

import javaposse.jobdsl.dsl.Job

class WashingMachineReleaseBranchJobBuilder extends AbstractTapasJobBuilder {

    protected def out
    protected String projectName

    protected Object releaseBranch

    protected String cil
    protected String jiveContext
    protected String jiveVersion
    protected String installnode
    protected String msv
    protected String targetHost
    protected String targetHost2
    protected String dlbHost
    protected String releaseBranchName
    protected String version
    protected String version2
    protected String dlbVersion
    protected String rpmversion
    protected String rpmversion2
    protected String runnfttest
    protected String gitbranch

    protected lockFileName = []
    protected String washingmachineReleasebranchSuffix = '_washingmachine_releasebranch'
    protected String rpmString = ""
    protected String configRpmString = ""
    protected boolean useRpmWm = false
    protected boolean isRpm = false
    protected boolean useDlb = false
    protected boolean useGitBranch = false

    public Job build() {
        lockFileName = suite.split('/')
        releaseBranchName = releaseBranch.@releasebranchname
        cil = releaseBranch.@cil
        msv = releaseBranch.@msv
        installnode = releaseBranch.@installnode
        version = releaseBranch.@version
        version2 = releaseBranch.@version2
        dlbVersion = releaseBranch.@dlbversion
        rpmversion = releaseBranch.@rpmversion
        rpmversion2 = releaseBranch.@rpmversion2
        jiveContext = releaseBranch.@jivecontext
        jiveVersion = releaseBranch.@jiveversion
        targetHost = releaseBranch.@targethost
        targetHost2 = releaseBranch.@targethost2
        dlbHost = releaseBranch.@dlbhost
        runnfttest = releaseBranch.@runnfttest
        if (useGitBranch) {
            gitbranch = releaseBranch.@gitbranch
        }

        if (isRpm) {
            washingmachineReleasebranchSuffix += '_rpm'
            rpmString = "Rpm "
            configRpmString = "_rpm"
            defaultTapasJobPath += '%20Rpm%20' + releaseBranchName
        } else {
            defaultTapasJobPath += '%20' + releaseBranchName
        }
        jobName = getProjectName()
        out.println("Creating " + jobName + " for releasebranch" + releaseBranchName)
        initProject(dslFactory.freeStyleJob(jobName))
        setProjectDescription(getProjectDescription())
        setRestrictLabel()
        addProjectConfig()
        discardOldBuilds(20, 20)
        deleteWorkspaceBeforeBuildStarts()
        addBuildParametersConfig()
        addTimeoutAndAbortConfig(timeoutForJob)
        if (useRpmWm && !isRpm) {
            //Do not start if RPM WM have managed to start, to avoid lockfile loop since this job triggers it self
            blockBuildIfJobsAreRunning(projectName + washingmachineReleasebranchSuffix + "_rpm_" + releaseBranchName, "GLOBAL", "DISABLED")
        }

        addPostBuildScripts()
        addTriggerParameterizedBuildOnOtherProjectsConfig(projectName)
        addArchiveArtifactsConfig(projectName)

        return job
    }

    protected void addProjectConfig() {
        job.with {
            wrappers {
                environmentVariables {
                    envs(injectVariables())
                    groovy(dslFactory.readFileFromWorkspace('scripts/InjectPortAllocation.groovy'))
                }
            }
            publishers { wsCleanup() }
        }
    }

    protected String getProjectDescription() {
        return getVersions() + ' ' + projectName.capitalize() + ' continuously to verify that everything works.'
    }

    protected String getVersions() {
        if (hasTwoTargethosts()) {
            return 'Installs ' + version + ' ' + 'and ' + version2
        }
        return 'Installs ' + version
    }

    protected String getProjectName() {
        return projectName + washingmachineReleasebranchSuffix + "_" + releaseBranchName
    }

    protected void addBuildParametersConfig() {
        job.with {
            parameters {
                if (isRpm) {
                    stringParam('RPMVERSION', rpmversion, '')
                    if (hasTwoTargethosts()) {
                        stringParam('RPMVERSION2', rpmversion2, '')
                    }
                } else {
                    stringParam('STAGINGVERSION', version, '')
                    if (hasTwoTargethosts()) {
                        stringParam('STAGINGVERSION2', version2, '')
                    }
                }
                stringParam('JIVEVERSION', jiveVersion, 'Version of JIVE to use, LATEST is default')
            }
        }
        getExtraBuildParameters()
    }

    protected void getExtraBuildParameters() {
        return
    }

    protected boolean hasTwoTargethosts() {
        return (targetHost2) ? true : false
    }

    @Override
    protected String getTapasParameters()
    {
        String params = ""
        if (installnode != "") {
            params += '--define=__INSTALLNODE__=' + installnode + ' \\\n'
        }
        params += '--define=__MSV__=' + msv + ' \\\n'
        params += '--define=__CIL__=' + cil + ' \\\n'
        params += '--define=__TARGETHOST__=' + targetHost + ' \\\n'
        params += '--define=__STAGING_VERSION__=' + version + ' \\\n'
        params += '--define=__RPM_VERSION__=' + rpmversion + ' \\\n'
        params += '--define=__BRANCH__=" Releasebranch ' + releaseBranchName + '" \\\n'
        if (hasTwoTargethosts()) {
            params += '--define=__TARGETHOST2__=' + targetHost2 + ' \\\n'
            params += '--define=__STAGING_VERSION2__=' + version2 + ' \\\n'
            params += '--define=__RPM_VERSION2__=' + rpmversion2 + ' \\\n'
        }
        if (useDlb) {
            params += '--define=__DLBHOST__=' + dlbHost + ' \\\n'
            params += '--define=__STAGING_VERSION3__=' + dlbVersion + ' \\\n'
        }
        params += '--define=__JIVEVERSION__=' + jiveVersion + ' \\\n'
        params += '--define=__JIVE_CONTEXT__=' + jiveContext + ' \\\n'
        params += '--define=__EXECUTION_HOST_PORT__=${EXECUTION_HOST_PORT} \\\n'
        params += '--define=__DISPLAYHOME__=${WORKSPACE} \\\n'
        params += '--define=__RUN_NFT_TEST__=' + runnfttest + ' \\\n'
        params += '--define=__SELENIUM_SUITE__=RmcaSeleniumSmokeSuite \\\n'
        if (useGitBranch) {
            params += '--define=__GIT_BRANCH__=' + gitbranch + ' \\\n'
        }
        params += getExtraTapasParameters()
        return params
    }

    protected String getExtraTapasParameters() {
        return ""
    }

    @Override
    protected String getAdditionalTapasAtExitShell() {
        String additionalTapas = '''
    # Find jive url
    jive_web_url=$(awk '/JiveURL/ {print $9}' \
    $WORKSPACE/tapasconsole.txt)
    echo "jive_web_url: $jive_web_url"
    echo "jive_web_url=$jive_web_url" >> $WORKSPACE/env.properties
'''

additionalTapas += '''
    ciscat_results=$(awk 'BEGIN {ORS=","};/CIS-CAT/ \
    {print $5 " " $6 " " $7}' $WORKSPACE/tapasconsole.txt | sed 's/,$//')
    echo "CISCAT_RESULT=${ciscat_results}" >> $WORKSPACE/env.properties
    echo $CISCAT_RESULT
'''
        return additionalTapas
    }

    @Override
    protected String getJenkinsDescription() {
        return '    echo \"JENKINS_DESCRIPTION $BUILDDESCRIPTION <a href=\\\"$jive_web_url\\\">Jive session</a> ' +
        '<a href=\\\"$tapas_web_url\\\">Tapas session</a>\"\n'
    }

    @Override
    protected String getTapasConfigSettings() {
        return 'BASE_CONFIG_FILE="/proj/eta-automation/tapas/config/' + projectName + '/' + suite + '"\n' +
               'CONFIG_FILE="/proj/eta-automation/tapas/sessions/' + projectName + '/' +
               lockFileName[-1].replace(configRpmString + '.xml', '_' + jiveContext + '.xml"')
    }

    protected Map injectVariables() {
        HashMap<String, String> env = [:]
        env.put("WORKSPACE_ORIG", "\${WORKSPACE}")
        env.put("WORKSPACE", getWorkspaceLocation())
        env.put("WS_TMP", getWorkspaceLocation() + "/.tmp")
        env.put('GIT_HOME', '/opt/local/dev_tools/git/latest')
        env.put('MAVEN_REPOSITORY', '${WORKSPACE}/.repository')
        env.put('GIT_CLONE_CACHE', '/workarea/bss-f_gen/kascmadm/.gitclonecache')
        env.put('MAVEN_SETTINGS', MAVEN_SETTINGS_PATH + 'kascmadm-settings_arm-' + projectName + '.xml')
        env.put('JAVA_TOOL_OPTIONS', '-Xms128M -Xmx3G -XX:MaxPermSize=256m -XX:SelfDestructTimer=45 -Djava.io.tmpdir=${WS_TMP}' +
                ' -DOSGI_PORT_OVERRIDE=\${ALLOCATED_PORT}')
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

    protected void addTriggerParameterizedBuildOnOtherProjectsConfig(String projectname) {
        job.with {
            publishers {
                downstreamParameterized {
                    trigger(projectname + '_washingmachine_blame') {
                        condition('FAILED_OR_BETTER')
                        parameters {
                            predefinedProp('JENKINS_URL', '$BUILD_URL')
                            predefinedProp('TAPAS_URL', '$tapas_web_url')
                            predefinedProp('JIVE_URL', '$jive_web_url')
                            predefinedProp('UPSTREAM_JOB', '$JOB_NAME')
                            predefinedProp('BLAME_CONFIG_FILE',
                                '/proj/eta-automation/tapas/config/' + projectname + '/config/blame/general-blame-config.json,' +
                                '/proj/eta-automation/blame_mail/wm-blame-config-' + projectname + '-' + releaseBranchName + '.json')
                            predefinedProp('STATUS', '$WM_BLAME_STATUS')
                            predefinedProp('CISCAT_RESULT', '$CISCAT_RESULT')
                            predefinedProp('DEFAULT_RECIPIENTS', projectname + '_washingmachine@mailman.lmera.ericsson.se')
                        }
                    }
                }
                if (useRpmWm && !isRpm) {
                    postBuildScripts {
                        steps {
                            conditionalSteps {
                                condition {
                                    status('ABORTED', 'FAILURE')
                                }
                                steps {
                                    downstreamParameterized {
                                        trigger(getProjectName()) {
                                            parameters {
                                                currentBuild()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        onlyIfBuildSucceeds(false)
                    }
                }
            }
        }
    }

    protected void addArchiveArtifactsConfig(String projectname) {
        job.with {
            publishers {
                archiveArtifacts {
                    pattern(projectname + 'rpms/*.rpm')
                    pattern('jive/*/*.jar')
                    pattern('*/jive/*.jar')
                    allowEmpty(true)
                    onlyIfSuccessful(false)
                    fingerprint(false)
                    defaultExcludes(true)
                }
            }
        }
    }

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

}

