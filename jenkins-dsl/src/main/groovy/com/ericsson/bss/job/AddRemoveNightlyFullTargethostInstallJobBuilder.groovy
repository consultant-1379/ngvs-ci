package com.ericsson.bss.job

import com.ericsson.bss.AbstractTapasJobBuilder
import javaposse.jobdsl.dsl.Job

public class AddRemoveNightlyFullTargethostInstallJobBuilder extends AbstractTapasJobBuilder {

    protected boolean onlyTargethostInstallation = false
    protected boolean useTwoTargethosts = false
    protected boolean useMsvMachine = true
    protected boolean useCilMachine = true
    protected jenkinsURL = ""
    protected List<String> resourceProfiles = []

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        setDescription()
        setConcurrentBuild(false)
        discardOldBuilds(30, 30)
        deleteWorkspaceBeforeBuildStarts()
        addTimeoutAndAbortConfig(240)
        addBuildParameters()
        addBuildSteps()
        deleteWorkspaceAfterBuild()
        return job
    }

    @Override
    protected void setDescription() {
        String setMsvCilVersion = (jenkinsURL + "job/" + tpgName +
                                   "_set_msv_cil_version")
        job.with {
            description(DSL_DESCRIPTION +
                '<h2>This job will add or remove your machines to the nightly targethostinstall ' +
                'list.</h2>It will run a full ' + tpgName + ' installation with latest OVF ' +
                'version.<br>If specified when adding a cluster, the nightly targethost install ' +
                'will also upgrade the CIL and/or MSV versions of each cluster to the version ' +
                'specified by <a href ="'+ setMsvCilVersion + '" />' + tpgName +
                '_set_msv_cil_version</a>.<br><br>Note that the cluster will be installed every ' +
                'night until it is removed from the list.')
        }
    }

    protected void addBuildParameters() {
        String createCluster = (jenkinsURL + "job/" + tpgName +
                                "_create_cluster")
        job.with {
            parameters {
                activeChoiceParam('ADDCLUSTER') {
                    groovyScript {
                        script(getAddClusterScript())
                        fallbackScript(defaultFallbackScript())
                    }
                    description("<b>Optional, use if you want automatic parameter fillment</b>" +
                                "<br> Available " + tpgName + " clusters, choose a cluster to add " +
                                "it in nightly installation. <br> Create your cluster " +
                                "<a href=\"" + createCluster + "\">here</a></b> <br> Do not touch this " +
                                "when using \"REMOVECLUSTER\" dropdownlist to remove cluster")
                }

                activeChoiceParam('REMOVECLUSTER') {
                    groovyScript {
                        script(getRemoveClusterScript())
                        fallbackScript(defaultFallbackScript())
                    }
                    description("Choose a cluster to remove from nightly installation, " +
                                "or manually add new cluster")
                }

                activeChoiceReactiveReferenceParam('TARGETHOST') {
                    choiceType('FORMATTED_HTML')
                    omitValueField()
                    groovyScript {
                        script(getHostScript('targethost'))
                        fallbackScript(defaultFallbackScript())
                    }
                    description("The machine(s) that should be deployed as Targethost.\n" +
                                "Multiple machines is separated by semicolon \";\"")
                    referencedParameter('ADDCLUSTER,REMOVECLUSTER')
                }

                if (useTwoTargethosts) {
                    activeChoiceReactiveReferenceParam('TARGETHOST2') {
                        choiceType('FORMATTED_HTML')
                        omitValueField()
                        groovyScript {
                            script(getHostScript('targethost2'))
                            fallbackScript(defaultFallbackScript())
                        }
                        description("The machine(s) that should be deployed as Targethost2.\n" +
                                    "Multiple machines is separated by semicolon \";\"")
                        referencedParameter('ADDCLUSTER,REMOVECLUSTER')
                    }
                }

                if (useMsvMachine) {
                    activeChoiceReactiveReferenceParam('MSV') {
                        choiceType('FORMATTED_HTML')
                        omitValueField()
                        groovyScript {
                            script(getHostScript('msv'))
                            fallbackScript(defaultFallbackScript())
                        }
                        description("MSV to use in targethost install")
                        referencedParameter('ADDCLUSTER,REMOVECLUSTER')
                    }
                }
                if (useCilMachine) {
                    activeChoiceReactiveReferenceParam('CIL') {
                        choiceType('FORMATTED_HTML')
                        omitValueField()
                        groovyScript {
                            script(getHostScript('cil'))
                            fallbackScript(defaultFallbackScript())
                        }
                        description("CIL to use in targethost install")
                        referencedParameter('ADDCLUSTER,REMOVECLUSTER')
                    }
                }

                activeChoiceReactiveReferenceParam('ACTION') {
                    choiceType('FORMATTED_HTML')
                    omitValueField()
                    groovyScript {
                        script(getActionScript())
                        fallbackScript(defaultFallbackScript())
                    }
                    description("ADD or REMOVE")
                    referencedParameter('ADDCLUSTER,REMOVECLUSTER')
                }

                activeChoiceReactiveReferenceParam('NAME') {
                    choiceType('FORMATTED_HTML')
                    omitValueField()
                    groovyScript {
                        script(getHostScript('name'))
                        fallbackScript(defaultFallbackScript())
                    }
                    description("Specify name (signum or team name) on your cluster, only " +
                                "possible when adding a cluster to the list")
                    referencedParameter('ADDCLUSTER,REMOVECLUSTER')
                }

                activeChoiceReactiveReferenceParam('USERMAIL') {
                    choiceType('FORMATTED_HTML')
                    omitValueField()
                    groovyScript {
                        script(getUsermailScript())
                        fallbackScript(defaultFallbackScript())
                    }
                    description("The EMAIL success/fail message should be sent to. " +
                                "User or team mail")
                    referencedParameter('ADDCLUSTER,REMOVECLUSTER,ACTION')
                }

                if (onlyTargethostInstallation) {
                    activeChoiceReactiveReferenceParam('INSTALLATIONTYPE') {
                        choiceType('FORMATTED_HIDDEN_HTML')
                        groovyScript {
                            script("return 'Full targethost install'")
                            fallbackScript(defaultFallbackScript())
                        }
                    }
                }
                else {
                    activeChoiceReactiveParam('INSTALLATIONTYPE') {
                        choiceType('SINGLE_SELECT')
                        groovyScript {
                            script(getInstallationTypeScript())
                            fallbackScript(defaultFallbackScript())
                        }
                        description("If the job should upgrade the MSV and/or CIL as well as " +
                                    "install the targethost." +
                                    "<br>Note that the MSV and CIL will only be upgraded if " +
                                    "there's a missmatch with the versions defined in " +
                                    "<a href ='" + jenkinsURL + "job/" + tpgName +
                                    "_set_msv_cil_version' />" + tpgName +
                                    "_set_msv_cil_version</a>.")
                        referencedParameter('ADDCLUSTER,REMOVECLUSTER')
                    }
                }

                activeChoiceReactiveReferenceParam('VMAPI_PREFIX') {
                    choiceType('FORMATTED_HTML')
                    omitValueField()
                    groovyScript {
                        script(getVmapiPrefixScript())
                        fallbackScript(defaultFallbackScript())
                    }
                    description("To use correct credentitials and vCenter. Normally the product " +
                                "the host belongs to. Ex: \"cil.\", \"charging.\", \"coba.\", " +
                                "\"cpm.\", \"ss7translator.\", \"invoicing.\".")
                    referencedParameter('ADDCLUSTER,REMOVECLUSTER')
                }
                choiceParam('RESOURCE_PROFILE', resourceProfiles, "Choose what resource profile should be used when installing the targethost")
            }
        }
        getOptionalInputParameters()
    }

    protected void getOptionalInputParameters() {
    }

    @Override
    protected void setTapasShell() {
    }

    @Override
    protected void archiveArtifacts() {
    }

    protected void addBuildSteps() {
        job.with {
            steps {
                systemGroovyCommand(getBuildScript())
            }
        }
    }

    protected String getAddClusterScript() {
        return "// Create a File object representing the file '<path><file>'\n" +
               "File file = new File('" + PATH_TO_JOB_CONFIG + "', '" +
                                     tpgName + "_cluster_list')\n" +
               "\n" +
               "// If it doesn't exist\n" +
               "if(!file.exists()) {\n" +
               "    return \"" + PATH_TO_JOB_CONFIG + tpgName +
                            "_cluster_list does not exist!\"\n" +
               "}\n" +
               "\n" +
               "XmlParser parser = new XmlParser()\n" +
               "groovy.util.Node clusters = parser.parse(file)\n" +
               "\n" +
               "ArrayList list = new ArrayList()\n" +
               "\n" +
               "clusters.children().each {\n" +
               "    list.add(it.@name)\n" +
               "}\n" +
               "\n" +
               "list.add(0,\"INPUT STRING\")\n" +
               "\n" +
               "return list\n"
    }

    protected String getRemoveClusterScript() {
        return "// Create a File object representing the file '<path><file>'\n" +
               "File file = new File('" + PATH_TO_JOB_CONFIG + "', '" +
                                     tpgName + "_nightly_list')\n" +
               "\n" +
               "// If it doesn't exist\n" +
               "if(!file.exists()) {\n" +
               "    return \"" + PATH_TO_JOB_CONFIG + tpgName +
                            "_nightly_list does not exist!\"\n" +
               "}\n" +
               "\n" +
               "XmlParser parser = new XmlParser()\n" +
               "groovy.util.Node clusters = parser.parse(file)\n" +
               "\n" +
               "ArrayList list = new ArrayList()\n" +
               "\n" +
               "clusters.children().each {\n" +
               "    list.add(it.@name)\n" +
               "}\n" +
               "\n" +
               "list.add(0,\"INPUT STRING\")\n" +
               "\n" +
               "return list\n"
    }

    protected String getHostScript(String type) {
        return "if(ADDCLUSTER.equals(\"INPUT STRING\")) {\n" +
               "    if(REMOVECLUSTER.equals(\"INPUT STRING\"))\n" +
               "    {\n" +
               "        return \"<input name=\\\"value\\\" value=\\\"\\\" " +
                                "class=\\\"setting-input\\\" type=\\\"text\\\">\"\n" +
               "    } \n" +
               "    else {\n" +
               "        // Create a File object representing the file '<path><file>'\n" +
               "        File file = new File('" + PATH_TO_JOB_CONFIG + "', '" +
                                             tpgName + "_nightly_list')\n" +
               "\n" +
               "        // If it doesn't exist\n" +
               "        if(!file.exists()) {\n" +
               "        return \"" + PATH_TO_JOB_CONFIG + tpgName +
                                "_nightly_list does not exist!\"\n" +
               "        }\n" +
               "\n" +
               "        XmlParser parser = new XmlParser()\n" +
               "        groovy.util.Node clusters = parser.parse(file)\n" +
               "\n" +
               "        String targethost_html = \"\"\n" +
               "\n" +
               "        clusters.children().find {\n" +
               "            if(REMOVECLUSTER.equals(it.@name)) {\n" +
               "                targethost_html = \"<input name=\\\"value\\\" value=\\\"\" + it.@" +
                                                   type + " + \"\\\" class=\\\"setting-input\\\" " +
                                                   "type=\\\"text\\\" disabled>\"\n" +
               "                return true\n" +
               "            }\n" +
               "            return false\n" +
               "        }\n" +
               "        return targethost_html\n" +
               "    }\n" +
               "}\n" +
               "else {\n" +
               "    // Create a File object representing the file '<path><file>'\n" +
               "    File file = new File('" + PATH_TO_JOB_CONFIG + "', '" +
                                         tpgName + "_cluster_list')\n" +
               "\n" +
               "    // If it doesn't exist\n" +
               "    if(!file.exists()) {\n" +
               "        return \"" + PATH_TO_JOB_CONFIG + tpgName +
                                "_cluster_list does not exist!\"\n" +
               "    }\n" +
               "\n" +
               "    XmlParser parser = new XmlParser()\n" +
               "    groovy.util.Node clusters = parser.parse(file)\n" +
               "\n" +
               "    String targethost_html = \"\"\n" +
               "\n" +
               "    clusters.children().find {\n" +
               "        if(ADDCLUSTER.equals(it.@name) && \n" +
               "            REMOVECLUSTER.equals(\"INPUT STRING\")) {\n" +
               "            targethost_html = \"<input name=\\\"value\\\" value=\\\"\" + it.@" +
                                              type + " + \"\\\" class=\\\"setting-input\\\" " +
                                              "type=\\\"text\\\" disabled>\"\n" +
               "            return true\n" +
               "        }\n" +
               "        return false\n" +
               "    }\n" +
               "    return targethost_html\n" +
               "} \n"
    }

    protected String getActionScript() {
        return "if(ADDCLUSTER.equals(\"INPUT STRING\")) {\n" +
               "    if(REMOVECLUSTER.equals(\"INPUT STRING\"))\n" +
               "    {\n" +
               "        return \"<input name=\\\"value\\\" value=\\\"ADD\\\" " +
                                "class=\\\"setting-input\\\" type=\\\"text\\\" disabled>\"\n" +
               "    }\n" +
               "    else {\n" +
               "        // Create a File object representing the file '<path><file>'\n" +
               "        File file = new File('" + PATH_TO_JOB_CONFIG + "', '" +
                                             tpgName + "_nightly_list')\n" +
               "\n" +
               "        // If it doesn't exist\n" +
               "        if(!file.exists()) {\n" +
               "            return \"\" \n" +
               "        }\n" +
               "\n" +
               "        XmlParser parser = new XmlParser()\n" +
               "        groovy.util.Node clusters = parser.parse(file)\n" +
               "\n" +
               "        String action_html = \"\"\n" +
               "\n" +
               "        clusters.children().find {\n" +
               "            if(REMOVECLUSTER.equals(it.@name)) {\n" +
               "                action_html = \"<input name=\\\"value\\\" value=\\\"REMOVE\\\" " +
                                               "class=\\\"setting-input\\\" type=\\\"text\\\" " +
                                               "disabled>\"\n" +
               "                return true\n" +
               "            }\n" +
               "            return false\n" +
               "        }\n" +
               "        return action_html\n" +
               "    }\n" +
               "}\n" +
               "else {\n" +
               "    // Create a File object representing the file '<path><file>'\n" +
               "    File file = new File('" + PATH_TO_JOB_CONFIG + "', '" +
                                         tpgName + "_cluster_list')\n" +
               "\n" +
               "    // If it doesn't exist\n" +
               "    if(!file.exists()) {\n" +
               "      return \"\"\n" +
               "    }\n" +
               "\n" +
               "    XmlParser parser = new XmlParser()\n" +
               "    groovy.util.Node clusters = parser.parse(file)\n" +
               "\n" +
               "    String action_html = \"\"\n" +
               "\n" +
               "    clusters.children().find {\n" +
               "        if(ADDCLUSTER.equals(it.@name) && \n" +
               "            REMOVECLUSTER.equals(\"INPUT STRING\")) {\n" +
               "            action_html = \"<input name=\\\"value\\\" value=\\\"ADD\\\" " +
                                           "class=\\\"setting-input\\\" type=\\\"text\\\" " +
                                           "disabled>\"\n" +
               "            return true\n" +
               "        }\n" +
               "        return false\n" +
               "    }\n" +
               "    return action_html\n" +
               "}"
    }

    protected String getUsermailScript() {
        return "if(ADDCLUSTER.equals(\"INPUT STRING\")) {\n" +
               "    if(ACTION.equals(\"ADD\"))\n" +
               "    {\n" +
               "        return \"<input name=\\\"value\\\" value=\\\"\\\" " +
                                "class=\\\"setting-input\\\" type=\\\"text\\\">\"\n" +
               "    }\n" +
               "    else {\n" +
               "        return\"<input name=\\\"value\\\" value=\\\"NOTNEEDED\\\" " +
                               "class=\\\"setting-input\\\" type=\\\"text\\\" disabled>\"\n" +
               "        // Create a File object representing the file '<path><file>'\n" +
               "    }\n" +
               "}\n" +
               "else {\n" +
               "    // Create a File object representing the file '<path><file>'\n" +
               "    File file = new File('" + PATH_TO_JOB_CONFIG + "', '" +
                                         tpgName + "_cluster_list')\n" +
               "\n" +
               "    // If it doesn't exist\n" +
               "    if(!file.exists()) {\n" +
               "        return \"\"\n" +
               "    }\n" +
               "\n" +
               "    XmlParser parser = new XmlParser()\n" +
               "    groovy.util.Node clusters = parser.parse(file)\n" +
               "\n" +
               "    String name_html = \"\"\n" +
               "\n" +
               "    clusters.children().find {\n" +
               "        if(ADDCLUSTER.equals(it.@name) &&\n" +
               "            REMOVECLUSTER.equals(\"INPUT STRING\")) {\n" +
               "            name_html = \"<input name=\\\"value\\\" value=\\\"\\\" " +
                                         "class=\\\"setting-input\\\" type=\\\"text\\\">\"\n" +
               "            return true\n" +
               "        }\n" +
               "        return false\n" +
               "    }\n" +
               "    return name_html \n" +
               "}"
    }

    protected String getVmapiPrefixScript() {
        return "if(ADDCLUSTER.equals(\"INPUT STRING\")) {\n" +
               "    if(REMOVECLUSTER.equals(\"INPUT STRING\"))\n" +
               "    {\n" +
               "        return \"<input name=\\\"value\\\" value=\\\"" + tpgName + ".\\\" " +
                                "class=\\\"setting-input\\\" type=\\\"text\\\">\"\n" +
               "    } \n" +
               "    else {\n" +
               "        // Create a File object representing the file '<path><file>'\n" +
               "        File file = new File('" + PATH_TO_JOB_CONFIG + "', '" +
                                             tpgName + "_nightly_list')\n" +
               "\n" +
               "        // If it doesn't exist\n" +
               "        if( !file.exists() ) {\n" +
               "        return \"" + PATH_TO_JOB_CONFIG + tpgName +
                                "_nightly_list does not exist!\"\n" +
               "        }\n" +
               "\n" +
               "        XmlParser parser = new XmlParser()\n" +
               "        groovy.util.Node clusters = parser.parse(file)\n" +
               "\n" +
               "        String targethost_html = \"\"\n" +
               "\n" +
               "        clusters.children().find {\n" +
               "            if( REMOVECLUSTER.equals( it.@name ) ) {\n" +
               "                String vmapiPrefix = it.@vmapiprefix\n" +
               "                if(!vmapiPrefix) {\n" +
               "                    vmapiPrefix = \"" + tpgName + ".\"\n" +
               "                }\n" +
               "                targethost_html = \"<input name=\\\"value\\\" value=\\\"\" + vmapiPrefix + \"\\\" " +
                                                  "class=\\\"setting-input\\\" type=\\\"text\\\" disabled>\"\n" +
               "                return true\n" +
               "            }\n" +
               "            return false\n" +
               "        }\n" +
               "        return targethost_html\n" +
               "    }\n" +
               "}\n" +
               "else {\n" +
               "        return \"<input name=\\\"value\\\" value=\\\"" + tpgName + ".\\\" " +
                                "class=\\\"setting-input\\\" type=\\\"text\\\">\"\n" +
               "} \n"
           }

    protected String getInstallationTypeScript() {
        return "String TARGETHOST_INSTALLATION = \"Full targethost install\"\n" +
               "String CIL_UPGRADE = \"Upgrade CIL and full targethost install\"\n" +
               "String MSV_CIL_UPGRADE = \"Upgrade MSV, CIL and full targethost install\"\n" +
               "\n" +
               "if(!REMOVECLUSTER.equals(\"INPUT STRING\")) {\n" +
               "    // Create a File object representing the file '<path><file>'\n" +
               "    File file = new File('" + PATH_TO_JOB_CONFIG + "', '" +
                                         tpgName + "_nightly_list')\n" +
               "\n" +
               "    // If it doesn't exist\n" +
               "    if(!file.exists()) {\n" +
               "        return [\"" + PATH_TO_JOB_CONFIG + tpgName +
                                "_nightly_list does not exist!\"]\n" +
               "    }\n" +
               "    XmlParser parser = new XmlParser()\n" +
               "    groovy.util.Node clusters = parser.parse(file)\n" +
               "\n" +
               "    String removeInstallationType = TARGETHOST_INSTALLATION\n" +
               "\n" +
               "    clusters.children().find {\n" +
               "        if(REMOVECLUSTER.equals(it.@name) && it.@installationtype) {\n" +
               "            if(it.@installationtype == \"cil\") {\n" +
               "                removeInstallationType = CIL_UPGRADE\n" +
               "            }\n" +
               "            else if(it.@installationtype == \"msv_cil\") {\n" +
               "                removeInstallationType = MSV_CIL_UPGRADE\n" +
               "            }\n" +
               "        }\n" +
               "    }\n" +
               "    return [removeInstallationType]\n" +
               "}\n" +
               "else {\n" +
               "    return [TARGETHOST_INSTALLATION, CIL_UPGRADE, MSV_CIL_UPGRADE]\n" +
               "}"
    }

    protected String getBuildScript() {
        String buildScript = ("import hudson.model.ParametersAction\n" +
                             "import hudson.model.FreeStyleBuild\n" +
                             "\n" +
                             "FreeStyleBuild build = Thread.currentThread().executable\n" +
                             "ParametersAction paramAction = build.getAction(ParametersAction)\n" +
                             "\n" +
                             "\n" +
                             "String TARGETHOST_INSTALLATION = \"Full targethost install\"\n" +
                             "String CIL_UPGRADE = \"Upgrade CIL and full targethost install\"\n" +
                             "String MSV_CIL_UPGRADE = \"Upgrade MSV, CIL and full targethost install\"\n" +
                             "\n" +
                             "\n" +
                             "//Get parameters\n" +
                             "String CLUSTER = " +
                                          "paramAction.getParameter('REMOVECLUSTER').getValue()\n" +
                             "String ADDCLUSTER = " +
                                          "paramAction.getParameter('ADDCLUSTER').getValue()\n" +
                             "String TARGETHOST = " +
                                          "paramAction.getParameter('TARGETHOST').getValue()\n")
        if (useTwoTargethosts) {
            buildScript += "String TARGETHOST2 = paramAction.getParameter('TARGETHOST2').getValue()\n"
        }
        if (useMsvMachine) {
            buildScript += "String MSV = paramAction.getParameter('MSV').getValue()\n"
        }
        if (useCilMachine) {
            buildScript += "String CIL = paramAction.getParameter('CIL').getValue()\n"
        }
        buildScript += ("String ACTION = paramAction.getParameter('ACTION').getValue()\n" +
                        "String NAME = paramAction.getParameter('NAME').getValue()\n" +
                        "String USERMAIL = paramAction.getParameter('USERMAIL').getValue()\n" +
                        "String INSTALLATIONTYPE = paramAction.getParameter('INSTALLATIONTYPE').getValue()\n")
        buildScript += getTpgSpecificParameters()
        buildScript += ("\n" +
                        "String VMAPI_PREFIX = paramAction.getParameter('VMAPI_PREFIX').getValue()\n" +
                        "String RESOURCE_PROFILE = paramAction.getParameter('RESOURCE_PROFILE').getValue()\n" +
                        "\n" +
                        "// Create a File object representing the file '<path><file>'\n" +
                        "File file = new File('" + PATH_TO_JOB_CONFIG + "', '" +
                                              tpgName + "_nightly_list')\n" +
                        "\n" +
                        "// If it doesn't exist\n" +
                        "if(!file.exists()) {\n" +
                        "    file.createNewFile()\n" +
                        "    StringWriter sw = new StringWriter()\n" +
                        "    groovy.xml.MarkupBuilder xml = new groovy.xml.MarkupBuilder(sw)\n" +
                        "    xml.clusters() { }\n" +
                        "    FileWriter fw = new FileWriter(file)\n" +
                        "    fw.write(sw.toString())\n" +
                        "    fw.close()\n" +
                        "}\n" +
                        "\n" +
                        "XmlParser parser = new XmlParser()\n" +
                        "groovy.util.Node clusters = null\n" +
                        "try {\n" +
                        "    clusters = parser.parse(file)\n" +
                        "}\n" +
                        "catch (Exception e) {\n" +
                        "    println (\"ERROR: \" + e)\n" +
                        "    return 1\n" +
                        "}\n" +
                        "\n" +
                        "println CLUSTER\n" +
                        "println ACTION\n" +
                        "println NAME\n" +
                        "\n" +
                        "boolean doWrite = false\n" +
                        "if (CLUSTER != 'INPUT STRING' && ADDCLUSTER != 'INPUT STRING'){\n" +
                        "    println \"DO NOT use both dropdowns together!!\"\n" +
                        "    return false\n" +
                        "}\n" +
                        "if (ACTION == 'REMOVE') {\n" +
                        "    println 'Removing from list'\n" +
                        "    groovy.util.Node hosts = clusters.find { it.@name == CLUSTER } \n" +
                        "    if (hosts) {\n" +
                        "        clusters.remove(hosts)\n" +
                        "        doWrite = true\n" +
                        "    }\n" +
                        "    else {\n" +
                        "        println \"No match, did not remove anything\"\n" +
                        "        return false\n" +
                        "    }\n" +
                        "}\n" +
                        "else {\n" +
                        "\n" +
                        "    groovy.util.Node hosts = clusters.find { it.@name == NAME }  \n" +
                        "    if (hosts) {\n" +
                        "        println \"JENKINS_DESCRIPTION The cluster is already added\"\n" +
                        "        return false\n" +
                        "    }\n" +
                        "\n" +
                        "    String installationType = \"tpg\"\n" +
                        "    if (INSTALLATIONTYPE == CIL_UPGRADE) {\n" +
                        "        installationType = \"cil\"\n" +
                        "    }\n" +
                        "    else if(INSTALLATIONTYPE == MSV_CIL_UPGRADE) {\n" +
                        "        installationType = \"msv_cil\"\n" +
                        "    }\n" +
                        "    parser.createNode(\n" +
                        "        clusters, 'cluster', \n" +
                        "        [\n" +
                        "            name:NAME, usermail:USERMAIL,\n" +
                        "            installationtype:installationType,\n" +
                        "            vmapiprefix:VMAPI_PREFIX, resourceprofile:RESOURCE_PROFILE, targethost:TARGETHOST,\n")
        if (useTwoTargethosts) {
            buildScript += "            targethost2:TARGETHOST2,\n"
        }
        if (useMsvMachine) {
            buildScript += "            msv:MSV,\n"
        }
        if (useCilMachine) {
            buildScript += "            cil:CIL,\n"
        }
        buildScript += ("            usermail:USERMAIL, installationtype:installationType, " +
                                     "vmapiprefix:VMAPI_PREFIX, resourceprofile:RESOURCE_PROFILE")
        buildScript += getTpgSpecificProperties()
        buildScript += ("\n" +
                        "        ]\n" +
                        "    )\n" +
                        "    doWrite = true\n" +
                        "}\n" +
                        "println clusters\n" +
                        "\n" +
                        "if (doWrite) {\n" +
                        "    FileWriter fileWriter = new FileWriter('" + PATH_TO_JOB_CONFIG +
                                                                    tpgName +
                                                                    "_nightly_list')\n" +
                        "    XmlNodePrinter nodePrinter = " +
                                            "new XmlNodePrinter(new PrintWriter(fileWriter))\n" +
                        "    nodePrinter.setPreserveWhitespace(true)\n" +
                        "    nodePrinter.print(clusters)\n" +
                        "    fileWriter.close()\n" +
                        "}\n")

        return buildScript
    }

    protected String getTpgSpecificParameters()
    {
        return ""
    }

    protected String getTpgSpecificProperties()
    {
        return ""
    }
}
