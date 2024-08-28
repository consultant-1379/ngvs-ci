package com.ericsson.bss.util

import com.ericsson.bss.AbstractJobBuilder

class Npm {
    public static final String SHELL_TO_GENERATE_SONAR_PROP_FILE =
        AbstractJobBuilder.getShellCommentDescription('Generate Sonarqube properties file') +
                'SONAR_PROPERTIES_FILE=sonar-runner.properties\n' +
                'PROJECT_FILE=package.json\n' +
                'PROJECT_NAME=`grep -oP \'(?<="name": ")[^"]*\' ${PROJECT_FILE} | ' +
                'sed \'s/[@]//g\' | sed \'s/[/]/./g\'`\n' +
                'PROJECT_VERSION=`grep -oP \'(?<="version": ")[^"]*\' ${PROJECT_FILE}`\n' +
                'echo sonar.projectKey=${PROJECT_NAME} >> ${SONAR_PROPERTIES_FILE}\n' +
                'echo sonar.projectName=${PROJECT_NAME} >> ${SONAR_PROPERTIES_FILE}\n' +
                'echo sonar.projectVersion=${PROJECT_VERSION} >> ${SONAR_PROPERTIES_FILE}\n' +
                'echo sonar.sources=. >> ${SONAR_PROPERTIES_FILE}'
}
