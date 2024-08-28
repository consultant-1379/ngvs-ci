package com.ericsson.bss.job.rmca

import com.ericsson.bss.job.UmiTestJobBuilder

class RmcaUmiTestJobBuilder extends UmiTestJobBuilder {

    @Override
    protected void getOptionalInputParameters() {
        job.with {
            parameters {
                stringParam('SUITE_SLOGAN', '',
                        'Optional. Should only be used for re-ocurring tests with a specific purpose that should be separated from other executions.')
                choiceParam('SKIPGUITEST', ['false', 'true'], 'Skip the selenium test for faster feedback.')
                stringParam('SIMULATORVERSION', 'LATEST', 'LATEST or a specific version will be fetched from ARM. Or a file specified will be used, the file ' +
                        'must be accessable from Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL.')
            }
        }
        addSharpVersionChoiceParam('SELENIUMVERSION', 'https://arm.epk.ericsson.se/artifactory/proj-rmca-dev-local/com/ericsson/bss/rmca/seleniumtest/',
                'Version of Selenium to use.')
    }

    @Override
    protected String getOptionalTapasParameters() {
        return '--define=__SELENIUMVERSION__=${SELENIUMVERSION} \\\n' +
               '--define=__SIMULATORVERSION__=${SIMULATORVERSION} \\\n' +
               '--define=__SUITE_SLOGAN__=${SUITE_SLOGAN} \\\n' +
               '--define=__DISPLAYHOME__=${WORKSPACE} \\\n' +
               '--define=__SKIP_GUI_TEST__=${SKIPGUITEST} \\\n' +
               '--define=__EXECUTION_HOST_PORT__=$((${ALLOCATED_PORT}+1)) \\\n'
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
            env_list["PATH"] = "\${GIT_HOME}/bin:\${M2}:\${FIREFOXDIR}:\${npm_config_prefix}/bin/:/opt/local/dev_tools" +
                    "/nodejs/node-v0.10.20-linux-x64/bin/:\${PATH}"
            injectEnv(env_list)
        }
    }
}
