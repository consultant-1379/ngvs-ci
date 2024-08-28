package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractTapasJobBuilder
import javaposse.jobdsl.dsl.Job

class WashingMachineUpgradeJobBuilder extends AbstractTapasJobBuilder {
    protected static final int DEFAULT_TIMEOUT = 240
    protected out
    protected String projectName

    protected String jiveVersion

    protected ArrayList variantArtifacts
    protected ArrayList targetHosts

    protected String uniqueConfigIdentifier

    public Job build() {
        jobName = projectName + "_washingmachine_upgrade"
        out.println("Creating washingmachine upgrade job for " + jobName)
        initProject(dslFactory.freeStyleJob(jobName))
        setProjectDescription(getProjectDescription())
        setRestrictLabel()
        discardOldBuilds(20, 20)
        deleteWorkspaceBeforeBuildStarts()
        deleteWorkspaceAfterBuild()
        addBuildParametersConfig()
        addTimeoutAndAbortConfig(DEFAULT_TIMEOUT)

        addPostBuildScripts()
        addTriggerParameterizedBuildOnOtherProjectsConfig(projectName)
        addArchiveArtifactsConfig(projectName)

        // Workaround for forced symlink in tapasbuilder
        Map env = [:]
        env.put("WORKSPACE_ORIG", "\${WORKSPACE}")
        env.put("WORKSPACE", getWorkspaceLocation())
        env.put("WS_TMP", getWorkspaceLocation() + "/.tmp/")
        injectEnv(env)
        // Remove above when bug fixed.
        return job
    }

    protected String getProjectDescription() {
        return 'Continuously run upgrade verification.'
   }

    protected void addBuildParametersConfig() {
        String toSingleLinePattern = ~/\s{2,}/
        String.metaClass.toSingleLine = {
            (delegate =~ toSingleLinePattern).replaceAll('')
        }
        out.println("variantArtifacts: " + variantArtifacts)
        job.with {
            parameters {
                stringParam('JIVEVERSION', 'RELEASED', 'Version of JIVE to use, RELEASED is default')
                targetHosts.each { host ->
                    activeChoiceParam("${host.name}") {
                        description("${host.description}")
                        choiceType('SINGLE_SELECT')
                        groovyScript {
                            script("""
                                return ["${host.defaultValue}"]
                            """.stripIndent())
                            fallbackScript('return ["Error evaluating Groovy script."]')
                        }
                    }
                }
                variantArtifacts.each {
                    String groupid = it.groupid.replaceAll('\\.', '/')
                    String releaseArtifactUrl = """
                        https://arm.epk.ericsson.se/
                        artifactory/proj-${projectName}-release-local/
                        ${groupid}/${it.artifactid}/maven-metadata.xml
                    """.toSingleLine()

                    String snapshotArtifactUrl = """
                        https://arm.epk.ericsson.se/
                        artifactory/proj-${projectName}-dev-local/
                        ${groupid}/${it.artifactid}/maven-metadata.xml
                    """.toSingleLine()

                    activeChoiceParam("${it.name.toUpperCase()}_FROM_VERSION") {
                        description("Version of A base image to upgrade from.")
                        choiceType('SINGLE_SELECT')
                        groovyScript {
                            script("""
                                GroovyObject metadata = new XmlSlurper().parse("${releaseArtifactUrl}")
                                version = metadata.versioning.latest
                                return ["\${version}"]
                            """.stripIndent())
                            fallbackScript('return ["Error evaluating Groovy script."]')
                        }
                    }

                    activeChoiceParam("${it.name.toUpperCase()}_TO_VERSION") {
                        description("Version of A base image to upgrade to.")
                        choiceType('SINGLE_SELECT')
                        groovyScript {
                            script("""
                                GroovyObject metadata = new XmlSlurper().parse("${snapshotArtifactUrl}")
                                version = metadata.versioning.latest
                                return ["\${version}"]
                            """.stripIndent())
                            fallbackScript('return ["Error evaluating Groovy script."]')
                        }
                    }
                }
            }
        }
    }

    @Override
    protected String getTapasParameters()
    {
        String params = ""
        targetHosts.each {
            params += "--define=__${it.name}__=\"${it.defaultValue}\" \\\n"
        }
        variantArtifacts.each {
            String variant = it.name.toUpperCase()
            params += "--define=__${variant}_FROM_VERSION__=\"\${${variant}_FROM_VERSION}\" \\\n"
            params += "--define=__${variant}_TO_VERSION__=\"\${${variant}_TO_VERSION}\" \\\n"
        }
        params += '--define=__JIVEVERSION__=${JIVEVERSION} \\\n'
        params += '--define=__EXECUTION_HOST_PORT__=${EXECUTION_HOST_PORT} \\\n'
        params += '--define=__DISPLAYHOME__=${WORKSPACE} \\\n'
        return params
    }

    @Override
    protected String getAdditionalTapasAtExitShell() {
        String additionalTapas = '''
            # Find jive url
            jive_web_url=$(awk '/JiveURL/ {print $9}' $WORKSPACE/tapasconsole.txt)
            echo "jive_web_url: $jive_web_url"
            echo "jive_web_url=$jive_web_url" >> env.properties

            ciscat_results=$(awk 'BEGIN {ORS=","};/CIS-CAT/ {print $5 " " $6 " " $7}' $WORKSPACE/tapasconsole.txt | sed 's/,$//')
            echo "CISCAT_RESULT=${ciscat_results}" >> env.properties
            echo $CISCAT_RESULT
        '''.stripIndent(8)
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
               'CONFIG_FILE="/proj/eta-automation/tapas/sessions/' + projectName + '/' + "washingmachine_upgrade-${uniqueConfigIdentifier}.xml\""
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
                                '/proj/eta-automation/blame_mail/wm-blame-config-' + projectname + '-' + 'upgrade' + '.json')
                            predefinedProp('STATUS', '$WM_BLAME_STATUS')
                            predefinedProp('CISCAT_RESULT', '$CISCAT_RESULT')
                            predefinedProp('DEFAULT_RECIPIENTS', projectname + '_washingmachine@mailman.lmera.ericsson.se')
                        }
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
