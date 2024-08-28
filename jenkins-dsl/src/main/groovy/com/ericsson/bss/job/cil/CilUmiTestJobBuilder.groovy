package com.ericsson.bss.job.cil

import com.ericsson.bss.job.UmiTestJobBuilder

class CilUmiTestJobBuilder extends UmiTestJobBuilder {

    @Override
    protected void setInputParameters() {
        addSelectClusterParameter('Choose cluster to update RPM on targethost')
        addClusterReferenceParameter('MSV', 'MSV for this cluster. The MSV used for information ' +
                                     'storage and deploy of the built OVF.')
        addClusterReferenceParameter('TARGETHOST', 'TARGETHOST for this cluster')
        addClusterReferenceParameter('INSTALLNODE', 'InstallNode for this cluster. The ' +
            'Installnode which performs the installation.')
        job.with {
            parameters {
                stringParam('STAGING', 'LATEST', 'LATEST or a specific version will be fetched from ARM. Or a' +
                            ' file specified will be used, the file must be accessable from ' +
                            'Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL. Note!' +
                            ' include_playlists.ini must point to correct playlist version ' +
                            '(SUF_PACKAGE_NAME in TPG section).')
                stringParam('PLAYLISTS', 'LATEST', 'LATEST or a specific version will be fetched from ARM. Or a' +
                            ' file specified will be used, the file must be accessable from ' +
                            'Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL.')
                stringParam('STAGINGDEPS', '', 'Optional. Use to override deps dowloaded with staging directory. The file must be accessable from Jenkins as' +
                        ' a filepath (ie /workarea/.. or /proj/..) or a URL. LATEST or a specific version will be fetched from ARM.')
                stringParam('TASKDIR', '', "Optional. If standard tasks should not be used. To give multiple dirs separate with space. Defaults to " +
                        "'/proj/env/tapastasks/'.")
                stringParam('CONFIGDIR', "/proj/eta-automation/tapas/config/" + projectName + "/config/",
                        'The location where the umi_test.xml file is located.')
                stringParam('VMAPIPROFILE', tpgName + '.', 'Specifies which credentials to use in VCenter.<br>Note: It should end with a dot!')
                stringParam('HOSTPROFILE', '', 'Specifies machine specific properties: vmfolder, hypervisorname, datastorename and network. If empty it will' +
                        ' match on just TARGETHOST.<br>Note: It should end with a dot!')
                stringParam('RPM', '', 'Optional. Specifies the CIL RPM. The file must be accessable from Jenkins as a filepath (ie /workarea/.. or /proj/.' +
                        '.) or a URL.')
            }

            getOptionalInputParameters()

            parameters {
                stringParam('JIVEVERSION', 'LATEST', 'Path to jive artifact, The file must be accessable from Jenkins as a filepath (ie /workarea/.. or ' +
                        '/proj/..) or a URL.')
                choiceParam('DO_ROLLBACK', ['true', 'false'], 'If true MSV and CIL will be rollbacked to snapshots.')
                choiceParam('NGEEVERSION', ['2', '1'], 'The version of NGEE being installed.')
            }
        }
        setEnvVariables()
    }

    @Override
    protected void getOptionalInputParameters() {
        job.with {
            parameters {
                stringParam('RPM2', '', 'Optional. Specifies the CASSANDRA RPM. The file must be accessable from Jenkins as a filepath (ie /workarea/.. or ' +
                        '/proj/..) or a URL.')
                stringParam('RPM3', '', 'Optional. Specifies the CILTOOL RPM. The file must be accessable from Jenkins as a filepath (ie /workarea/.. or ' +
                        '/proj/..) or a URL.')
                stringParam('RPM4', '', 'Optional. Specifies the CILCLI RPM. The file must be accessable from Jenkins as a filepath (ie /workarea/.. or ' +
                        '/proj/..) or a URL.')
                stringParam('RPM5', '', 'Optional. Specifies the DISKINIT RPM. The file must be accessable from Jenkins as a filepath (ie /workarea/.. or ' +
                        '/proj/..) or a URL.')
                stringParam('DEPLOY', 'LATEST', 'The file that contain the deploy scripts to be tested. The file must be accessable from Jenkins as a ' +
                        'filepath (ie /workarea/.. or /proj/..) or a URL. LATEST will get the latest from ARM. Or the specified above will be used. ')
            }
        }
    }

    @Override
    protected String getOptionalTapasParameters() {
        return  '--define=__RPM2__="\${RPM2}" \\\n' +
                '--define=__RPM3__="\${RPM3}" \\\n' +
                '--define=__RPM4__="\${RPM4}" \\\n' +
                '--define=__RPM5__="\${RPM5}" \\\n' +
                '--define=__DEPLOY__="\${DEPLOY}" \\\n'
    }

    @Override
    protected void setEnvVariables() {
        job.with {
            Map env_list = getInjectVariables()
            env_list.remove("HOME")
            env_list.put("WORKSPACE_ORIG", "\${WORKSPACE}")
            env_list.put("WORKSPACE", "/tmp/jenkins-ka-ln/\${JOB_NAME}/\${MESOS_EXECUTOR_NUMBER}")
            env_list.put("CDT_INSTALL_FOLDER", "/tmp/jenkins-ka-ln/\${JOB_NAME}/\${EXECUTOR_NUMBER}")
            env_list.put("FIREFOXDIR", "/proj/eta-tools/firefox/45.0esr/Linux_i386_64/firefox/firefox")
            env_list.put("npm_config_cache", "\${WS_TMP}/npm_config_cache")
            env_list.put("npm_config_prefix", "\${CDT_INSTALL_FOLDER}/node_modules")
            env_list["WS_TMP"] = "/tmp/jenkins-ka-ln/\${JOB_NAME}/\${MESOS_EXECUTOR_NUMBER}/.tmp"
            env_list["MAVEN_REPOSITORY"] = "\${WORKSPACE}/.repository"
            env_list["MAVEN_SETTINGS"] = MAVEN_SETTINGS_PATH + "kascmadm-settings_arm-rmca.xml"
            env_list["JAVA_TOOL_OPTIONS"] = JAVA_TOOL_OPTIONS + " -DOSGI_PORT_OVERRIDE=\$OSGI_PORT_OVERRIDE"
            env_list["MAVEN_OPTS"] = MAVEN_OPTS + " -DOSGI_PORT_OVERRIDE=\$OSGI_PORT_OVERRIDE"
            env_list["PATH"] = "\${GIT_HOME}/bin:\${M2}:\${FIREFOXDIR}:\${npm_config_prefix}/bin/:" +
                    "/opt/local/dev_tools/nodejs/node-v0.10.20-linux-x64/bin/:\${PATH}"
            injectEnv(env_list)
        }
    }
}
