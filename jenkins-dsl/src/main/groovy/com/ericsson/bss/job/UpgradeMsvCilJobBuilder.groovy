package com.ericsson.bss.job

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.util.Email
import javaposse.jobdsl.dsl.Job

public class UpgradeMsvCilJobBuilder extends AbstractJobBuilder {

    protected String setUpgradeStatus = 'scripts/set_upgrade_status.sh'
    protected String jenkins
    protected String type
    protected String tpgName = ""

    private static final String GERRIT_JIVE_CLUSTER_FOLDER = "/proj/eta-automation/jenkins/kascmadm/clusters/"

    public Job build() {
        if (tpgName.equals("")) {
            tpgName = this.projectName.split("\\.")[0]
        }
        initProject(dslFactory.freeStyleJob(jobName))
        addConfig()
        addBuildParameters()
        addBuildSteps()
        configureEmailNotification()
        super.setJenkinsUserBuildVariables()
        return job
    }

    private void addConfig() {
        job.with {
            description(DSL_DESCRIPTION +
                "<h2>This job upgrades MSV and CIL hosts of chosen cluster</h2>")
            concurrentBuild()

            def variables = getInjectVariables()
            if (type.equals("gerrit_jive")) {
                String productFolder = tpgName
                if (tpgName.equals("charging")) {
                    productFolder = "chargingcore"
                }
                variables.put("PRODUCT_FOLDER", productFolder)
            }
            injectEnv(variables)
        }

    }

    private void addBuildParameters() {
        job.with {
            parameters {
                activeChoiceParam('CLUSTER') {
                    groovyScript {
                        if (type.equals("gerrit_jive")) {
                            script(getGerritJiveClusterScript())
                        } else {
                            script(getClusterScript())
                        }
                    }
                    description('Optional. Use if you want to upgrade a predefined cluster.')
                }

                activeChoiceReactiveReferenceParam('MSV') {
                    choiceType('FORMATTED_HTML')
                    omitValueField()
                    groovyScript {
                        if (type.equals("gerrit_jive")) {
                            script(getGerritJiveHostScript('msv'))
                        } else {
                            script(getHostScript('msv'))
                        }
                    }
                    referencedParameter('CLUSTER')
                    description('Which MSV machine should be upgraded.')
                }

                activeChoiceParam('MSVVERSION') {
                    choiceType('SINGLE_SELECT')
                    groovyScript {
                        script(getMsvVersions())
                    }
                    description('Which MSV version the selected machine should be upgraded to.')
                }

                choiceParam('MSVRESOURCEPROFILE',
                    ["TestSystem", "TeamMachine", "Default", "Washingmachine", "Extended"],
                    "Specifies how much hardware resources (CPU and RAM) the MSV should be " +
                    "deployed with. Normally the \"TestSystem\" profile should be used. ")

                booleanParam('INSTALLMSV', true, 'If MSV should be installed or not')

                activeChoiceReactiveReferenceParam('CIL') {
                    choiceType('FORMATTED_HTML')
                    omitValueField()
                    groovyScript {
                        if (type.equals("gerrit_jive")) {
                            script(getGerritJiveHostScript('cil'))
                        } else {
                            script(getHostScript('cil'))
                        }
                    }
                    referencedParameter('CLUSTER')
                    description('Which CIL machine should be upgraded.')
                }
            }
        }
        addVersionChoiceParam('CILVERSION',
                              'https://arm.epk.ericsson.se/artifactory/proj-cil-release-local/com/ericsson/bss/cil/server/cil-server-dv,' +
                              'https://arm.epk.ericsson.se/artifactory/proj-cil-release-local/com/ericsson/bss/cil/server/cil-staging/',
                              'Which CIL version the selected machine should be upgraded to.')
        job.with {
            parameters {
                choiceParam('CILRESOURCEPROFILE',
                    ["TeamMachine", "Washingmachine", "TestSystem", "Standard"],
                    "Specifies how much hardware resources (CPU and RAM) the CIL should be " +
                    "deployed with. Normally the \"TeamMachine\" profile should be used. ")

                stringParam('VMAPI_PREFIX', tpgName + '.', 'To use correct credentitials and ' +
                                                            'vCenter. Normally the product the ' +
                                                            'host belongs to. Ex: "cil.", ' +
                                                            '"charging.", "coba.", "cpm.", ' +
                                                            '"ss7translator.", "invoicing.".')
            }
        }
    }

    private void addBuildSteps() {
        if (type.equals("gerrit_jive")) {
            job.with {
                steps {
                    shell(setUpgradeStatusScript())
                }
            }
        }

        switch (jenkins) {
            case 'internal':
                addInternalBuildSteps()
                break
            case 'eforge':
                addEforgeBuildSteps()
                break
            default:
                addOtherJenkinsBuildSteps()
                break
        }

        if (type.equals("gerrit_jive")) {
            job.with {
                steps {
                    conditionalSteps {
                        condition {
                            status('SUCCESS', 'SUCCESS')
                        }
                        steps {
                            shell(removeUpgradeStatusScript())
                        }
                    }
                }
            }
        }
    }

    private void addInternalBuildSteps() {
        job.with {
            steps {
                conditionalSteps {
                    condition {
                        booleanCondition('\${INSTALLMSV}')
                    }
                    steps {
                        downstreamParameterized {
                            trigger('msv_ovf_deploy') {
                                block {
                                    buildStepFailure('FAILURE')
                                    failure('FAILURE')
                                    unstable('UNSTABLE')
                                }
                                parameters {
                                    predefinedProps([TARGETHOST: '\$MSV',
                                                     MSVVERSION: '\$MSVVERSION',
                                                     VMAPI_PREFIX: '\$VMAPI_PREFIX',
                                                     PRODUCT: tpgName,
                                                     RESOURCE_PROFILE: '\$MSVRESOURCEPROFILE'])
                                }
                            }
                        }
                    }
                }
                remoteTrigger('eforge', 'cil_targethost_install') {
                    parameters(TARGETHOST: '\$CIL',
                               VERSION: '\$CILVERSION',
                               MSV: '\$MSV',
                               VMAPI_PREFIX: '\$VMAPI_PREFIX',
                               PRODUCT: tpgName,
                               RESOURCE_PROFILE: '\$CILRESOURCEPROFILE',
                               USERMAIL: '\$BUILD_USER_EMAIL')
                    blockBuildUntilComplete()
                }
            }
        }
    }

    private void addEforgeBuildSteps() {
        job.with {
            steps {
                remoteTrigger('Internal Jenkins', 'msv_ovf_deploy') {
                    parameters(TARGETHOST: '\$MSV',
                               MSVVERSION: '\$MSVVERSION',
                               VMAPI_PREFIX: '\$VMAPI_PREFIX',
                               PRODUCT: tpgName,
                               RESOURCE_PROFILE: '\$MSVRESOURCEPROFILE',
                               USERMAIL: '\$BUILD_USER_EMAIL')
                    blockBuildUntilComplete()
                }
                downstreamParameterized {
                    trigger('cil_targethost_install') {
                        block {
                            buildStepFailure('FAILURE')
                            failure('FAILURE')
                            unstable('UNSTABLE')
                        }
                        parameters {
                           predefinedProps([TARGETHOST: '\$CIL',
                                            VERSION: '\$CILVERSION',
                                            MSV: '\$MSV',
                                            VMAPI_PREFIX: '\$VMAPI_PREFIX',
                                            PRODUCT: tpgName,
                                            RESOURCE_PROFILE: '\$CILRESOURCEPROFILE'])
                        }
                    }
                }
            }
        }
    }

    private void addOtherJenkinsBuildSteps() {
        job.with {
            steps {
                remoteTrigger('Internal Jenkins', 'msv_ovf_deploy') {
                    parameters(TARGETHOST: '\$MSV',
                               MSVVERSION: '\$MSVVERSION',
                               VMAPI_PREFIX: '\$VMAPI_PREFIX',
                               PRODUCT: tpgName,
                               RESOURCE_PROFILE: '\$MSVRESOURCEPROFILE',
                               USERMAIL: '\$BUILD_USER_EMAIL')
                    blockBuildUntilComplete()
                }
                remoteTrigger('eforge', 'cil_targethost_install') {
                    parameters(TARGETHOST: '\$CIL',
                               VERSION: '\$CILVERSION',
                               MSV: '\$MSV',
                               VMAPI_PREFIX: '\$VMAPI_PREFIX',
                               PRODUCT: tpgName,
                               RESOURCE_PROFILE: '\$CILRESOURCEPROFILE',
                               USERMAIL: '\$BUILD_USER_EMAIL')
                    blockBuildUntilComplete()
                }
            }
        }
    }

    private String getClusterScript() {
        return "// Create a File object representing the file '<path><file>'\n" +
               "def file = new File('" + PATH_TO_JOB_CONFIG + "', '" + tpgName + "_cluster_list')\n" +
               "// If it doesn't exist\n" +
               "if ( !file.exists() ) {\n" +
               "  return \"\"\n" +
               "}\n" +
               "def parser = new XmlParser()\n" +
               "def clusters = parser.parse(file)\n" +
               "def list = []\n" +
               "clusters.children().each {\n" +
               "  if (!it.@name.toLowerCase().contains('jive')) {\n" +
               "    list.add(it.@name)\n" +
               "  }\n" +
               "}\n" +
               "list.add(0,\"INPUT STRING\")\n" +
               "return list\n"
    }

    private String getHostScript(String type) {
        return "if (CLUSTER.equals(\"INPUT STRING\"))\n" +
               "{\n" +
               "  return \"<input name=\\\"value\\\" value=\\\"\\\" class=\\\"setting-input\\\" type=\\\"text\\\">\"\n" +
               "} else {\n" +
               "    // Create a File object representing the file '<path><file>'\n" +
               "    def file = new File('" + PATH_TO_JOB_CONFIG + "', '" + tpgName + "_cluster_list')\n" +
               "    // If it doesn't exist\n" +
               "    if ( !file.exists() ) {\n" +
               "      return \"\"\n" +
               "    }\n" +
               "    def parser = new XmlParser()\n" +
               "    def clusters = parser.parse(file)\n" +
               "    def targethost_html = \"\"\n" +
               "    clusters.children().find {\n" +
               "    if ( CLUSTER.equals( it.@name ) ) {\n" +
               "      targethost_html = \"<input name=\\\"value\\\" value=\\\"\" + it.@" + type +
               " + \"\\\" class=\\\"setting-input\\\" type=\\\"text\\\" disabled>\"\n" +
               "      return true\n" +
               "    }\n" +
               "    return false\n" +
               "  }\n" +
               "  return targethost_html\n" +
               "}"
    }

    private String getGerritJiveClusterScript() {
        String subFolder = tpgName
        if (tpgName.equals("charging")) {
            subFolder = "chargingcore"
        }
        return "import groovy.io.FileType\n" +
               "def list = []\n" +
               "new File(\"" + GERRIT_JIVE_CLUSTER_FOLDER + getSubFolder() +
               "/config\").eachFileRecurse (FileType.FILES) { list << it.name.split(\"\\\\.\")[0] }\n" +
               "list.removeAll { !it.contains('cluster') }\n" +
               "list = list.sort() { a,b ->\n" +
               "  def numA = (a - 'cluster').toInteger()\n" +
               "  def numB = (b - 'cluster').toInteger()\n" +
               "  return numA <=> numB\n" +
               "}\n" +
               "return list "
    }

    private String getSubFolder() {
        String subFolder = tpgName
        if (tpgName.equals("charging")) {
            subFolder = "chargingcore"
        }
        return subFolder
    }

    private String getGerritJiveHostScript(String type) {
        return "import groovy.json.JsonSlurper\n" +
               "// Create a File object representing the file '<path><file>'\n" +
               "def file = new File('" + GERRIT_JIVE_CLUSTER_FOLDER + getSubFolder() + "/config', CLUSTER + '.json')\n" +
               "// If it doesn't exist\n" +
               "if ( !file.exists() ) {\n" +
               "  return \"\"\n" +
               "}\n" +
               "def json = new JsonSlurper().parseText(file.text)\n" +
               "def targethost_html = targethost_html = \"<input name=\\\"value\\\" value=\\\"\" + json[CLUSTER]." + type +
               " + \"\\\" class=\\\"setting-input\\\" type=\\\"text\\\" disabled>\"\n" +
               "return targethost_html"
    }

    private String getMsvVersions() {
        return  "def version_list = []\n" +
                "def file = new File('" + PATH_TO_JOB_CONFIG + "', 'msv_version_list')\n" +
                "if ( !file.exists() ) {\n" +
                "  return \"\"\n" +
                "}\n" +
                "def parser = new XmlParser()\n" +
                "def versions = parser.parse(file)\n" +
                "versions.children().each {\n" +
                "  version_list.add(it.@ver)\n" +
                "}\n" +
                "return version_list"
    }

    private String setUpgradeStatusScript() {
        return dslFactory.readFileFromWorkspace(setUpgradeStatus)
    }

    private String removeUpgradeStatusScript() {
        return "rm -f " + GERRIT_JIVE_CLUSTER_FOLDER + getSubFolder() + "/status/\$CLUSTER.upgrade"
    }

    private void configureEmailNotification() {
        Email email = Email.newBuilder().withRecipient("\$BUILD_USER_EMAIL")
                .withContent("\$BUILD_URL")
                .withSubject("\$DEFAULT_SUBJECT")
                .withFailureTrigger("Upgrade failed: \$JOB_NAME #\$BUILD_NUMBER")
                .withFixedTrigger("Upgrade successful: \$JOB_NAME #\$BUILD_NUMBER")
                .withAbortedTrigger("Upgrade aborted: \$JOB_NAME #\$BUILD_NUMBER")
                .build()

        addEmailNotificationConfig(email)
    }
}
