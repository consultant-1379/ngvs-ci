common_env_variables = [
        'WORKSPACE_ORIG'    : '${WORKSPACE}',
        'WORKSPACE'         : '/tmp/jenkins-ka-ln/\${JOB_NAME}/\${MESOS_EXECUTOR_NUMBER}',
        'WS_TMP'            : '/tmp/jenkins-ka-ln/\${JOB_NAME}/\${MESOS_EXECUTOR_NUMBER}/.tmp/',
        'GIT_HOME'          : '/opt/local/dev_tools/git/latest',
        'MAVEN_REPOSITORY'  : '${WORKSPACE}/.repository',
        'GIT_CLONE_CACHE'   : '/workarea/bss-f_gen/kascmadm/.gitclonecache',
        'MAVEN_SETTINGS'    : '/proj/eta-automation/maven/kascmadm-settings_arm-charging.xml',
        'JAVA_TOOL_OPTIONS' : '-Xms128M -Xmx3G -XX:MaxPermSize=256m -XX:SelfDestructTimer=45 -Djava.io.tmpdir=${WORKSPACE}/.tmp/ -DOSGI_PORT_OVERRIDE=\${ALLOCATED_PORT}',
        'M2_HOME'           : '/opt/local/dev_tools/maven/apache-maven-3.2.1',
        'M2'                : '${M2_HOME}/bin',
        'MAVEN_OPTS'        : '-Xms128M -Xmx3G -XX:MaxPermSize=256m -XX:SelfDestructTimer=45 -Djava.io.tmpdir=${WORKSPACE}/.tmp/ -DOSGI_PORT_OVERRIDE=\${ALLOCATED_PORT}',
        'CDT_INSTALL_FOLDER': '/tmp/jenkins-ka-ln/${JOB_NAME}/${MESOS_EXECUTOR_NUMBER}',
        'FIREFOXDIR'        : '/proj/eta-tools/firefox/45.0esr/Linux_i386_64/firefox/firefox',
        'npm_config_cache'  : '${WS_TMP}/npm_config_cache',
        'npm_config_prefix' : '${CDT_INSTALL_FOLDER}/node_modules',
        'PATH'              : '${GIT_HOME}/bin:${M2}:${FIREFOXDIR}:${npm_config_prefix}/bin/:/opt/local/dev_tools/nodejs/node-v0.10.20-linux-x64/bin/:${PATH}'
]
