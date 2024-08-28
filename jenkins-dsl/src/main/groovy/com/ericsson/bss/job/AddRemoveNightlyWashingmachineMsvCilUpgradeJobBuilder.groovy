package com.ericsson.bss.job

import com.ericsson.bss.AbstractTapasJobBuilder
import javaposse.jobdsl.dsl.Job

public class AddRemoveNightlyWashingmachineMsvCilUpgradeJobBuilder extends AbstractTapasJobBuilder {

    protected boolean useTwoTargethosts = false
    protected String jenkinsURL = ""

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
        String setMsvCilVersion = jenkinsURL + "job/" + tpgName + "_set_msv_cil_version/"

        job.with {
            description(DSL_DESCRIPTION +
                '<h2>This job adds or removes Washingmachines for MSV and CIL upgrading</h2>\n' +
                'Specifiy the desired MSV and CIL for a specific washingmachine branch with ' +
                '<a href ="'+ setMsvCilVersion + '" />' + tpgName + '_set_msv_cil_version</a>. ' +
                '\n<br>\nThe added washingmachines will only be upgraded if there is a missmatch between ' +
                'this version and the actual version.\n\n<br>\n\n<br>\nNote that the washingmachine will be ' +
                'installed every night until it is removed from the list.')
        }
    }

    protected void addBuildParameters() {
        job.with {
            parameters {
                activeChoiceParam('ADDBRANCH') {
                    groovyScript {
                        script(getAddBranchScript())
                        fallbackScript(defaultFallbackScript())
                    }
                    description("<br>Available " + tpgName + " branches, choose a branch to add " +
                                "it in nightly washingmachine MSV and CIL installation.")
                }

                activeChoiceParam('REMOVEBRANCH') {
                    groovyScript {
                        script(getRemoveBranchScript())
                        fallbackScript(defaultFallbackScript())
                    }
                    description("Choose a branch to remove from nightly washingmachine MSV/CIL installation")
                }
                activeChoiceReactiveReferenceParam('ACTION') {
                    choiceType('FORMATTED_HTML')
                    omitValueField()
                    groovyScript {
                        script(getActionScript())
                        fallbackScript(defaultFallbackScript())
                    }
                    description("ADD or REMOVE")
                    referencedParameter('ADDBRANCH,REMOVEBRANCH')
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
                    referencedParameter('ADDBRANCH,REMOVEBRANCH')
                }
                activeChoiceReactiveParam('INSTALLATIONTYPE') {
                    choiceType('SINGLE_SELECT')
                    groovyScript {
                        script(getInstallationTypeScript())
                        fallbackScript(defaultFallbackScript())
                    }
                    description("If the job should upgrade the MSV and/or CIL as well as install " +
                                "the targethost")
                    referencedParameter('ADDBRANCH,REMOVEBRANCH,ACTION')
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
                    referencedParameter('ADDBRANCH,REMOVEBRANCH')
                }
                activeChoiceReactiveReferenceParam('CLUSTERS') {
                    choiceType('FORMATTED_HTML')
                    omitValueField()
                    groovyScript {
                        script(getClustersInfoScript())
                        fallbackScript(defaultFallbackScript())
                    }
                    referencedParameter('ADDBRANCH,REMOVEBRANCH')
                }
            }
        }
        getOptionalInputParameters()
    }

    protected void getOptionalInputParameters() {
        return
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

    protected String getAddBranchScript() {
        return "ArrayList list = new ArrayList()\n" +
               "list.add('-')\n" +
               "\n" +
               "File file = new File('" + PATH_TO_JOB_CONFIG + "', '" + tpgName + "_branches_list')\n" +
               "if (!file.exists()) {\n" +
               "    return 'File does not exist'\n" +
               "}\n" +
               "\n" +
               "XmlParser parser = new XmlParser()\n" +
               "Node clusters = parser.parse(file)\n" +
               "\n" +
               "clusters.children().each {\n" +
               "    list.add(it.@branch)\n" +
               "}\n" +
               "\n" +
               "file = new File('" + PATH_TO_JOB_CONFIG + "', '" + tpgName + "_release_branch_washingmachine')\n" +
               "if (!file.exists()) {\n" +
               "    return 'File does not exist'\n" +
               "}\n" +
               "\n" +
               "clusters = parser.parse(file)\n" +
               "\n" +
               "clusters.children().each {\n" +
               "    list.add('releasebranch_' + it.@releasebranchname)\n" +
               "}\n" +
               "\n" +
               "return list"
    }

    protected String getRemoveBranchScript() {
        return "// Create a File object representing the file '<path><file>'\n" +
               "String file_name = '" + tpgName + "_nightly_washingmachine_list'\n" +
               "File file = new File('" + PATH_TO_JOB_CONFIG + "', file_name)\n" +
               "\n" +
               "// If it doesn't exist\n" +
               "if( !file.exists() ) {\n" +
               "    return ['-']\n" +
               "}\n" +
               "\n" +
               "XmlParser parser = new XmlParser()\n" +
               "Node clusters = parser.parse(file)\n" +
               "ArrayList list = new ArrayList()\n" +
               "\n" +
               "list.add('-')\n" +
               "clusters.children().each {\n" +
               "    list.add(it.@branch)\n" +
               "}\n" +
               "\n" +
               "return list"
    }

    protected String getActionScript() {
        return "if (ADDBRANCH.equals('-')) {\n" +
               "    if (REMOVEBRANCH.equals('-')) {\n" +
               "        return '<input name=\"value\" value=\"ADD\" class=\"setting-input\" " +
                               "type=\"text\" disabled>'\n" +
               "    }\n" +
               "    else {\n" +
               "        return '<input name=\"value\" value=\"REMOVE\" class=\"setting-input\" " +
                               "type=\"text\" disabled>'\n" +
               "    }\n" +
               "}\n" +
               "else {\n" +
               "    return '<input name=\"value\" value=\"ADD\" class=\"setting-input\" " +
                           "type=\"text\" disabled>'\n" +
               "}"
    }

    protected String getUsermailScript() {
        return "if (ADDBRANCH.equals('-')) {\n" +
               "    if (REMOVEBRANCH.equals('-')) {\n" +
               "        return '<input name=\\\"value\\\" value=\\\"-\\\" " +
                               "class=\\\"setting-input\\\" type=\\\"text\\\" disabled>'\n" +
               "    }\n" +
               "    else {\n" +
               "        String nightly_file_name = '" + tpgName + "_nightly_washingmachine_list'\n"+
               "        File nightly_file = new File('" + PATH_TO_JOB_CONFIG + "', " +
                                                     "nightly_file_name)\n" +
               "        if( !nightly_file.exists() ) {\n" +
               "            return '<input name=\\\"value\\\" value=\\\"-\\\" " +
                                   "class=\\\"setting-input\\\" type=\\\"text\\\" disabled>'\n" +
               "        }\n" +
               "\n" +
               "        XmlParser parser = new XmlParser()\n" +
               "        Node nightly_clusters = parser.parse(nightly_file)\n" +
               "        String html_value = '-'\n" +
               "        nightly_clusters.find {\n" +
               "            if (REMOVEBRANCH == it.@branch) {\n" +
               "                html_value = it.@usermail\n" +
               "                return true\n" +
               "            }\n" +
               "        }\n" +
               "        return '<input name=\\\"value\\\" value=\\\"' + html_value + '\\\" " +
                               "class=\\\"setting-input\\\" type=\\\"text\\\" disabled>'\n" +
               "    }\n" +
               "}\n" +
               "else {\n" +
               "    return '<input name=\\\"value\\\" value=\\\"\\\" class=\\\"setting-input\\\" " +
                           "type=\\\"text\\\">'\n" +
               "}"
    }

    protected String getVmapiPrefixScript() {
        return "if(ADDBRANCH.equals(\"INPUT STRING\")) {\n" +
               "    if(REMOVEBRANCH.equals(\"INPUT STRING\"))\n" +
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
               "            if( REMOVEBRANCH.equals( it.@name ) ) {\n" +
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

   protected String getClustersInfoScript() {
        return "import groovy.xml.MarkupBuilder\n" +
                "\n" +
                "StringWriter writer = new StringWriter()\n" +
                "XmlParser parser = new XmlParser()\n" +
                "MarkupBuilder builder = new MarkupBuilder(writer)\n" +
                "\n" +
                "if(ADDBRANCH.equals('-')) {\n" +
                "    if(REMOVEBRANCH.equals('-')) {\n" +
                "        return ''\n" +
                "    }\n" +
                "    else {\n" +
                "        File file = new File('" + PATH_TO_JOB_CONFIG + "', '" + tpgName + "_nightly_washingmachine_list')\n" +
                "        if (!file.exists()) {\n" +
                "            return ''\n" +
                "        }\n" +
                "\n" +
                "        groovy.util.Node clusters = parser.parse(file)\n" +
                "        builder.span {\n" +
                "            h3(REMOVEBRANCH + ':')\n" +
                "            clusters.children().each { cluster->\n" +
                "                if (cluster.@branch != REMOVEBRANCH) {\n" +
                "                    return\n" +
                "                }\n" +
                "                ul() {\n" +
                "                    cluster.attributes().each{ k, v ->\n" +
                "                        if (k != 'branch' && k != 'usermail') {\n" +
                "                            li(k + ': ' + v)\n" +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        return writer.toString()\n" +
                "    }\n" +
                "}\n" +
                "else {\n" +
                "    \n" +
                "    File file = new File('" + PATH_TO_JOB_CONFIG + "', '" + tpgName + "_branches_list')\n" +
                "    if (!file.exists()) {\n" +
                "        return ''\n" +
                "    }\n" +
                "\n" +
                "    builder.span {\n" +
                "        h3(ADDBRANCH + ':')\n" +
                "    }\n" +
                "\n" +
                "    clusters = parser.parse(file)\n" +
                "    Boolean foundBranch = false\n" +
                "    builder.span {\n" +
                "        clusters.children().each { cluster ->\n" +
                "            if (cluster.@branch != ADDBRANCH) {\n" +
                "                return\n" +
                "            }\n" +
                "            foundBranch = true\n" +
                "            ul() {\n" +
                "                cluster.attributes().each { k, v ->\n" +
                "                    if ( k != 'branch' && k != 'cluster') {\n" +
                "                        li(k + ': ' + v)\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    if (!foundBranch) {\n" +
                "        file = new File('" + PATH_TO_JOB_CONFIG + "', '" + tpgName + "_release_branch_washingmachine')\n" +
                "        if (!file.exists()) {\n" +
                "            return ''\n" +
                "        }\n" +
                "\n" +
                "        clusters = parser.parse(file)\n" +
                "        builder.span {\n" +
                "            clusters.children().each { cluster ->\n" +
                "                if (('releasebranch_' + cluster.@releasebranchname) != ADDBRANCH) {\n" +
                "                    return\n" +
                "                }\n" +
                "                ul() {\n" +
                "                    cluster.attributes().each { k, v ->\n" +
                "                        if (k != 'version' && k != 'releasebranchname' && k != 'jiveversion' \n" +
                "                            && k != 'jivecontext' && k != 'jivesuitename') {\n" +
                "                            li(k + ': ' + v)\n" +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "    }\n" +
                "    return writer.toString()\n" +
                "}"
   }

    protected String getInstallationTypeScript() {
        return "String CIL_UPGRADE = \"Upgrade CIL and full targethost install\"\n" +
               "String MSV_CIL_UPGRADE = \"Upgrade MSV, CIL and full targethost install\"\n" +
               "ArrayList list = [CIL_UPGRADE, MSV_CIL_UPGRADE]\n" +
               "\n" +
               "if (ACTION.equals('ADD')) {\n" +
               "    if(ADDBRANCH.equals('NONE')) {\n" +
               "        return ['NONE':'-']\n" +
               "    }\n" +
               "    return list\n" +
               "}\n" +
               "else {\n" +
               "    String nightly_file_name = '" + tpgName + "_nightly_washingmachine_list'\n" +
               "    File nightly_file = new File('" + PATH_TO_JOB_CONFIG + "', " +
                                                 "nightly_file_name)\n" +
               "    if( !nightly_file.exists() ) {\n" +
               "        return false\n" +
               "    }\n" +
               "\n" +
               "    XmlParser parser = new XmlParser()\n" +
               "    Node nightly_clusters = parser.parse(nightly_file)\n" +
               "    String removeInstallationType = \"\"\n" +
               "\n" +
               "    nightly_clusters.find {\n" +
               "        if (it.@branch == REMOVEBRANCH) {\n" +
               "            if(it.@installationtype == \"cil\") {\n" +
               "                removeInstallationType = CIL_UPGRADE\n" +
               "                return true\n" +
               "            }\n" +
               "            else if(it.@installationtype == \"msv_cil\") {\n" +
               "                removeInstallationType = MSV_CIL_UPGRADE\n" +
               "                return true\n" +
               "            }\n" +
               "        }\n" +
               "    }\n" +
               "    if(removeInstallationType) {\n" +
               "        return [removeInstallationType]\n" +
               "    }\n" +
               "    return list\n" +
               "}"
    }

    protected String getBuildScript() {
        return "import hudson.model.ParametersAction\n" +
               "import hudson.model.FreeStyleBuild\n" +
               "\n" +
               "FreeStyleBuild build = Thread.currentThread().executable\n" +
               "ParametersAction paramAction = build.getAction(ParametersAction)\n" +
               "\n" +
               "String CIL_UPGRADE = \"Upgrade CIL and full targethost install\"\n" +
               "String MSV_CIL_UPGRADE = \"Upgrade MSV, CIL and full targethost install\"\n" +
               "\n" +
               "String ADDBRANCH = paramAction.getParameter('ADDBRANCH').getValue()\n" +
               "String REMOVEBRANCH = paramAction.getParameter('REMOVEBRANCH').getValue()\n" +
               "String USERMAIL = paramAction.getParameter('USERMAIL').getValue()\n" +
               "String ACTION = paramAction.getParameter('ACTION').getValue()\n" +
               "String INSTALLATIONTYPE = paramAction.getParameter('INSTALLATIONTYPE').getValue()\n" +
               "String VMAPI_PREFIX = paramAction.getParameter('VMAPI_PREFIX').getValue()\n" +
               "boolean doWrite = false\n" +
               "\n" +
               "String nightly_file_name = '" + tpgName + "_nightly_washingmachine_list'\n" +
               "File nightly_file = new File('" + PATH_TO_JOB_CONFIG + "', " +
                                             "nightly_file_name)\n" +
               "if( !nightly_file.exists() ) {\n" +
               "    nightly_file.createNewFile()\n" +
               "    StringWriter sw = new StringWriter()\n" +
               "    groovy.xml.MarkupBuilder xml = new groovy.xml.MarkupBuilder(sw)\n" +
               "    xml.clusters() { }\n" +
               "    FileWriter fw = new FileWriter(nightly_file)\n" +
               "    fw.write(sw.toString())\n" +
               "    fw.close()\n" +
               "}\n" +
               "\n" +
               "XmlParser parser = new XmlParser()\n" +
               "groovy.util.Node nightly_clusters = null\n" +
               "try {\n" +
               "    nightly_clusters = parser.parse(nightly_file)\n" +
               "}\n" +
               "catch (Exception e) {\n" +
               "    println (\"ERROR: \" + e)\n" +
               "    return false\n" +
               "}\n" +
               "\n" +
               "if (ADDBRANCH != '-' && REMOVEBRANCH != '-') {\n" +
               "    println ('Do NOT use both dropdowns together')\n" +
               "    return false\n" +
               "}\n" +
               "if (ADDBRANCH == '-' && REMOVEBRANCH == '-') {\n" +
               "    println ('select one of the dropdowns')\n" +
               "    return false\n" +
               "}\n" +
               "\n" +
               "if (ACTION == 'REMOVE') {\n" +
               "    ArrayList remove_clusters = nightly_clusters.findAll {\n" +
               "        it.@branch == REMOVEBRANCH\n" +
               "    }\n" +
               "    if (remove_clusters) {\n" +
               "        remove_clusters.each {\n" +
               "            nightly_clusters.remove(it)\n" +
               "        }\n" +
               "        doWrite = true\n" +
               "    }\n" +
               "    else {\n" +
               "        println ('No match, did not remove anything')\n" +
               "        return false\n" +
               "    }\n" +
               "}\n" +
               "else {\n" +
               "    String branch_file_name = '" + tpgName + "_branches_list'\n" +
               "    File branch_file = new File('" + PATH_TO_JOB_CONFIG + "', " +
                                                "branch_file_name)\n" +
               "    if( !branch_file.exists() ) {\n" +
               "        return false\n" +
               "    }\n" +
               "    \n" +
               "    groovy.util.Node branch_clusters = parser.parse(branch_file)\n" +
               "    ArrayList branch_clusters_list = new ArrayList()\n" +
               "    branch_clusters.each {\n" +
               "        if (it.@branch == ADDBRANCH) {\n" +
               "            branch_clusters_list.add(it)\n" +
               "        }\n" +
               "    }\n" +
               "\n" +
               "    if (branch_clusters_list.isEmpty()) {\n" +
               "        branch_file = new File('" + PATH_TO_JOB_CONFIG +
                                               tpgName + "_release_branch_washingmachine')\n" +
               "        if (!branch_file.exists()) {\n" +
               "            return false\n" +
               "        }\n" +
               "\n" +
               "        branch_clusters = parser.parse(branch_file)\n" +
               "        branch_clusters.each {\n" +
               "            String branch_name = 'releasebranch_' + it.@releasebranchname\n" +
               "            if (branch_name == ADDBRANCH) {\n" +
               "                it.@branch = branch_name\n" +
               "                branch_clusters_list.add(it)\n" +
               "            }\n" +
               "        }\n" +
               "    }\n" +
               "    boolean addClusters = true\n" +
               "    branch_clusters_list.each {\n" +
               "        nightly_clusters.each { cluster ->\n" +
               "            if (it.@branch == cluster.@branch) {\n" +
               "                addClusters = false\n" +
               "                return\n" +
               "            }\n" +
               "        }\n" +
               "        if(!addClusters) {\n" +
               "            return\n" +
               "        }\n" +
               "    }\n" +
               "    if (!addClusters) {\n" +
               "        println ('The branch is already added')\n" +
               "        return false\n" +
               "    }\n" +
               "    println USERMAIL\n" +
               "    String installationType = \"\"\n" +
               "    if(INSTALLATIONTYPE == CIL_UPGRADE) {\n" +
               "        installationType = \"cil\"\n" +
               "    }\n" +
               "    else if(INSTALLATIONTYPE == MSV_CIL_UPGRADE) {\n" +
               "        installationType = \"msv_cil\"\n" +
               "    }\n" +
               "    HashMap<String,String> clusterAttributes = new HashMap()\n" +
               "    branch_clusters_list.each {\n" +
               "        clusterAttributes = [branch:it.@branch, targethost:it.@targethost, cil:it.@cil,\n" +
               "                             msv:it.@msv, usermail:USERMAIL, installationtype:installationType,\n" +
               "                             vmapiprefix:VMAPI_PREFIX]\n" +
               "        if (it.@targethost2) {\n" +
               "            clusterAttributes.put('targethost2', it.@targethost2)\n" +
               "        }\n" +
               "\n" +
               "        parser.createNode(nightly_clusters, 'cluster', clusterAttributes)\n" +
               "    }\n" +
               "    doWrite = true\n" +
               "}\n" +
               "\n" +
               "if (doWrite) {\n" +
               "    println nightly_clusters\n" +
               "    FileWriter fileWriter = new FileWriter('" + PATH_TO_JOB_CONFIG + "' " +
                                                           "+ nightly_file_name)\n" +
               "    XmlNodePrinter nodePrinter = new XmlNodePrinter(new PrintWriter(fileWriter))\n"+
               "    nodePrinter.setPreserveWhitespace(true)\n" +
               "    nodePrinter.print(nightly_clusters)\n" +
               "    fileWriter.close()  \n" +
               "}"
    }
}
