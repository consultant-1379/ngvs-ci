package com.ericsson.bss.job.cel

import com.ericsson.bss.job.SonarJobBuilder

class CelSonarJobBuilder extends SonarJobBuilder {

    @Override
    protected void initShellJobs() {
        shells.add(cleanUpWorkspaceMesos("\${WORKSPACE}"))
        shells.add(removeOldArtifacts())
        shells.add(gitConfig("\${WORKSPACE}"))
        if (generateGUIconfig) {
            shells.add(gconfWorkspaceWorkaround())
        }
        shells.add(getJacocoCommand())
        shells.add(getSeleniumInstallCommand())
        shells.add(getCDTSonarTesReportWorkaround())
    }

    private String getJacocoCommand() {
        return '' +
                getShellCommentDescription("Maven jacoco command") +
                'mvn \\\n' +
                '-P gui org.jacoco:jacoco-maven-plugin:0.7.4.201502262128:prepare-agent install \\\n' +
                '-DparallelTests \\\n' +
                '-B -e \\\n' +
                '-Dsurefire.useFile=false \\\n' +
                '-Dcompiler.version=1.7 \\\n' +
                '-Dmaven.repo.local=${MAVEN_REPOSITORY} -Dorg.ops4j.pax.url.mvn.localRepository=${MAVEN_REPOSITORY} \\\n' +
                '-Dorg.ops4j.pax.url.mvn.settings=${MAVEN_SETTINGS} --settings ${MAVEN_SETTINGS} \\\n' +
                '-DOSGI_PORT_OVERRIDE=\${ALLOCATED_PORT} \\\n' +
                '-Dmaven.test.failure.ignore=true'
    }

    private String getSeleniumInstallCommand() {
        return '' +
                getShellCommentDescription("Build selenium without the test. Will allways fail because no backend.") +
                'mvn \\\n' +
                'clean install -DskipTests \\\n' +
                '-f ui/selenium/ \\\n' +
                '-B -e \\\n' +
                '-Dsurefire.useFile=false \\\n' +
                '-Dcompiler.version=1.7 \\\n' +
                '-DsingleThreadTest -Dcobertura.skip=true -Djarsigner.skip=true \\\n' +
                '-Dmaven.repo.local=${MAVEN_REPOSITORY} -Dorg.ops4j.pax.url.mvn.localRepository=${MAVEN_REPOSITORY} \\\n' +
                '-Dorg.ops4j.pax.url.mvn.settings=${MAVEN_SETTINGS} --settings ${MAVEN_SETTINGS} \\\n' +
                '-DOSGI_PORT_OVERRIDE=\${ALLOCATED_PORT}'
    }
}
