package com.ericsson.bss.job.cel

import com.ericsson.bss.job.GerritSonarJobBuilder

class CelGerritSonarJobBuilder extends GerritSonarJobBuilder {

    @Override
    protected void addJobs() {
        job.with {
            steps {
                shell(gitInitiateRepository("\${WORKSPACE}"))
                shell(gitConfig("\${WORKSPACE}"))
                shell(getGitCache("\${WORKSPACE}"))
                shell(gitFetchGerritChange())
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
                groovyCommand(verifyCommitMessageScript(), GROOVY_INSTALLATION_NAME)
                shell(gerritReportCommitMessage())
                shell(removeOldArtifacts())
                if (generateGUIconfig) {
                    shell(gconfWorkspaceWorkaround())
                }
                shell(getJacocoCommand())
                shell(getSeleniumInstallCommand())
                shell(SONAR_ANALYSIS_LOG_OUTPUT)
                if (generateGUIconfig) {
                    shell(getCDTSonarTesReportWorkaround())
                }
                shell(getMavenSonarCommand())
                String mavenProjectFolder = mavenProjectLocation && mavenProjectLocation.length() > POM_XML.length() ?
                        mavenProjectLocation[0..-POM_XML.length()]:""
                shell(getSonarPosterCommand(mavenProjectFolder))
                if (mavenProjectFolder) {
                    shell("# Copy the issues-report " +
                            "\nmkdir -p target/sonar/issues-report ; cp -r " + mavenProjectFolder +
                            "/target/sonar/issues-report/* target/sonar/issues-report")
                }
            }
        }

        job.with {
            steps {
                shell(grepCommitMessageIssues())
                shell(copySitePreview("./target/sonar/issues-report/*", "sonar", projectName))
                shell(grepSonarIssues())
            }
        }
    }

    @Override
    protected String getMavenSonarCommand() {
        return getShellCommentDescription("Maven sonar command") +
                "mvn \\\n" +
                "-P gui,selenium \\\n" +
                sonarMavenPlugin + ":sonar -Dsonar.issuesReport.console.enable=true -Dsonar.issuesReport.html.enable=true " +
                "-Dsonar.analysis.mode=incremental -Dsonar.profile=\"BMARP Design Rules\" \\\n" +
                "-Dsonar.host.url=https://sonar.epk.ericsson.se \\\n" +
                "-Dsurefire.useFile=false \\\n" +
                getMavenGeneralBuildParameters() + " \\\n" +
                ">> console_out.txt \\\n"
    }

    private String getJacocoCommand() {
        return getShellCommentDescription("Maven jacoco command") +
                'mvn \\\n' +
                'clean -P gui clean org.jacoco:jacoco-maven-plugin:0.7.4.201502262128:prepare-agent install \\\n' +
                '-DparallelTests \\\n' +
                '-B -e \\\n' +
                '-Dsurefire.useFile=false \\\n' +
                '-Dcompiler.version=1.7 \\\n' +
                '-Dmaven.repo.local=${MAVEN_REPOSITORY} -Dorg.ops4j.pax.url.mvn.localRepository=${MAVEN_REPOSITORY} \\\n' +
                '-Dorg.ops4j.pax.url.mvn.settings=${MAVEN_SETTINGS} --settings ${MAVEN_SETTINGS} \\\n' +
                '-DOSGI_PORT_OVERRIDE=\${ALLOCATED_PORT}'
    }

    private String getSeleniumInstallCommand() {
        return getShellCommentDescription("Build selenium without the test. Will allways fail because no backend.") +
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

    @Override
    protected String gerritFeedbackFail() {
        String gerritFeedbackFail = dslFactory.readFileFromWorkspace('scripts/gerrit_sonar_fail_feedback.sh')
        gerritFeedbackFail = gerritFeedbackFail.replace('GERRIT_USER="%CODE_QUALITY_USER%"',
                addGerritUser(jenkinsCodeQualityUser)).replaceAll('%GERRIT_SERVER%', gerritServer)
        String sitePreview = getSitePreviewUrl("sonar", projectName) + "'\${GERRIT_PATCHSET_REVISION}'/target/sonar/issues-report/issues-report" +
                ".html"
        gerritFeedbackFail = gerritFeedbackFail.replace("%SITE_PREVIEW%", sitePreview)

        if (isUsingLabelForReviews()) {
            gerritFeedbackFail = gerritFeedbackFail
                    .replace("-l Code-Review=", "--label Code-Quality=")
        }

        return gerritFeedbackFail
    }
}
