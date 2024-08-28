package com.ericsson.bss.job.rmca

import com.ericsson.bss.AbstractJobBuilder

/**
 * A helper class to perform common tasks and avoid code duplication between
 * RmcaSeleniumJobBuilder and RmcaGerritSeleniumJobBuilder
 */
class RmcaSeleniumHelper {

    private AbstractJobBuilder ref

    public RmcaSeleniumHelper(AbstractJobBuilder ref) {
        this.ref = ref
    }

    public void addCommonSeleniumConfig(String profile) {
        this.ref.job.with {
            description(this.ref.DSL_DESCRIPTION + 'This job compiles and start a ' +
                    'backend. It then runs the selenium "' + profile + '" test suite against that backend.')
            customWorkspace(this.ref.CUSTOM_WORKSPACE_MESOS)
            this.ref.addTimeoutConfig()

            this.ref.injectEnv(this.ref.getInjectVariables())
        }
    }

    public void addKillBackendCommand() {
        this.ref.job.with {
            publishers {
                flexiblePublish {
                    conditionalAction {
                        condition { alwaysRun() }
                        steps { shell(getBackendKillCommand()) }
                    }
                }
            }
        }
    }

    public String getSedCommand() {
        return "sed -i ':a;N;\$!ba;s/ *\\\"test\\\",\\n//g' ui/*/build.json"
    }

    public String getBuildCommand() {
        return this.ref.getShellCommentDescription("Maven backend build command") +
                'mvn clean install -DskipTests -Pgui \\\n' +
                '-B -e  \\\n' +
                '-Dcobertura.skip=true -Djarsigner.skip=true \\\n' +
                '-Dsurefire.useFile=false -Dmaven.repo.local=\${MAVEN_REPOSITORY} \\\n' +
                '-Dorg.ops4j.pax.url.mvn.localRepository=\${MAVEN_REPOSITORY} \\\n' +
                '-Dorg.ops4j.pax.url.mvn.settings=\${MAVEN_SETTINGS} --settings \${MAVEN_SETTINGS} \\\n' +
                '-DOSGI_PORT_OVERRIDE=\${ALLOCATED_PORT}'
    }

    public String getRunBackendCommand() {
        return '#!/bin/sh\n' +
                this.ref.getShellCommentDescription("Run headless backend container") +
                'mvn clean test -Dtest=LaunchGuiDevEnvTest -Dhttp_rest_port=\$((\${ALLOCATED_PORT}+1)) -Dbase.http.port=\$((\${ALLOCATED_PORT}+2)) ' +
                '-f integrationtest/devenv/ \\\n' +
                '-B -e  \\\n' +
                '-Dcobertura.skip=true -Djarsigner.skip=true \\\n' +
                '-Dsurefire.useFile=false -Dmaven.repo.local=\${MAVEN_REPOSITORY} \\\n' +
                '-Dorg.ops4j.pax.url.mvn.localRepository=\${MAVEN_REPOSITORY} \\\n' +
                '-Dorg.ops4j.pax.url.mvn.settings=\${MAVEN_SETTINGS} --settings \${MAVEN_SETTINGS} \\\n' +
                '-DOSGI_PORT_OVERRIDE=\${ALLOCATED_PORT} > \${WORKSPACE}/karaf_instance.log &' +
                '\n' +
                '\n' +
                this.ref.getShellCommentDescription("Store backend pid in file") +
                'echo "\$!" > \${WORKSPACE}/karaf_instace.pid'
    }

    public String getSeleniumCommand(String profile) {
        return this.ref.getShellCommentDescription("Sleep so backend container is started") +
                'sleep 60\n' +
                '\n' +
                this.ref.getShellCommentDescription("Run selenium test") +
                'mvn clean package -f selenium/ -Dselenium.base-url=http://127.0.0.1:\$((\${ALLOCATED_PORT}+2))/ui -Dmaven.test.failure.ignore=true \\\n' +
                '-P' + profile + ' \\\n' +
                '-B -e  \\\n' +
                '-Dcobertura.skip=true -Djarsigner.skip=true \\\n' +
                '-Dsurefire.useFile=false -Dmaven.repo.local=\${MAVEN_REPOSITORY} \\\n' +
                '-Dorg.ops4j.pax.url.mvn.localRepository=\${MAVEN_REPOSITORY} \\\n' +
                '-Dorg.ops4j.pax.url.mvn.settings=\${MAVEN_SETTINGS} --settings \${MAVEN_SETTINGS} \\\n' +
                '-DOSGI_PORT_OVERRIDE=\${ALLOCATED_PORT}'
    }

    public String getSeleniumBuildCommand() {
        return this.ref.getShellCommentDescription("Sleep so backend container is started") +
                'sleep 60\n' +
                '\n' +
                this.ref.getShellCommentDescription("Build selenium test") +
                'mvn clean install -f selenium/ -DskipTests'
    }

    public String getUseCaseRecorderCommand() {
        return this.ref.getShellCommentDescription("Record the use case tests") +
                '/usr/bin/time -v /proj/env/etacommon/bin/jive_use_case_recorder.py -vv -c ${WORKSPACE}/selenium/target/ || true'
    }

    public String uploadVideosToARM() {
        return this.ref.getShellCommentDescription("Get maven properties") +
                'VERSION=$(mvn help:evaluate -Dexpression=project.version | grep -Ev "(^\\[|Download\\w+:)")\n' +
                'ARTIFACT_ID=$(mvn help:evaluate -Dexpression=project.artifactId | grep -Ev "(^\\[|Download\\w+:)")\n' +
                'GROUP_ID=$(mvn help:evaluate -Dexpression=project.groupId | grep -Ev "(^\\[|Download\\w+:)")\n' +
                'URL=$(mvn help:evaluate -Dexpression=project.distributionManagement.repository.url | grep -Ev "(^\\[|Download\\w+:)")\n' +
                'if [[ $VERSION =~ .*-SNAPSHOT.* ]]\n' +
                'then\n' +
                '    URL=$(mvn help:evaluate -Dexpression=project.distributionManagement.snapshotRepository.url | grep -Ev "(^\\[|Download\\w+:)")\n' +
                'fi\n' +
                '\n' +
                'zip recordings.zip *.avi\n' +
                '\n' +
                this.ref.getShellCommentDescription("Upload the videos to ARM") +
                '/opt/local/dev_tools/maven/latest/bin/mvn deploy:deploy-file -Dfile=recordings.zip -Dversion=${VERSION} -DgroupId=${GROUP_ID} ' +
                '-DartifactId=${ARTIFACT_ID} -Dclassifier=recordings -DgeneratePom=false -Durl=${URL} -DrepositoryId=arm\n'
    }

    public String getBackendKillCommand() {
        return this.ref.getShellCommentDescription("Kill backend") +
                'if [ -f "\${WORKSPACE}/karaf_instace.pid" ]\n' +
                'then\n' +
                'kill -9 $(cat \${WORKSPACE}/karaf_instace.pid) || echo "Ignore error if Jenkins already managed to close process"\n' +
                'fi'
    }
}
