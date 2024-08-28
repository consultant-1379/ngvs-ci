package com.ericsson.bss.job

import com.ericsson.bss.AbstractGerritJobBuilder

import javaposse.jobdsl.dsl.Job

public class RemoveReleaseBranchWashingMachineJobBuilder extends AbstractGerritJobBuilder {

    private String projectName
    protected boolean useRpmWm = false

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addRemoveBranchConfig()
        return job
    }

    public void addRemoveBranchConfig() {
        job.with {

            String jobDescription = "<h2>Remove release branch washingmachine for " + projectName + ".</h2>"

            description(DSL_DESCRIPTION + jobDescription)
            concurrentBuild()
            parameters {
                activeChoiceParam('REMOVEABLE_BRANCHES') {
                    description('Choose branch to remove')
                    groovyScript {
                        script(removableBranches())
                        fallbackScript('return ["Error evaluating Groovy script."]')
                    }
                }
            }

            steps {
                systemGroovyCommand(removeBranchGroovy())
                systemGroovyCommand(removeBranchBlameMail())
                systemGroovyCommand(removeDefaultJiveSessionGroovy())
            }
        }
    }

    private String removableBranches() {
        return "    // Create a File object representing the file '<path><file>'\n" +
                "    def file = new File('" + PATH_TO_JOB_CONFIG + "', '" + projectName + "_release_branch_washingmachine')\n\n" +
                "// If it doesn't exist\n" +
                "    if ( !file.exists() ) {\n" +
                "        return \"\"\n" +
                "    }\n\n" +
                "    def parser = new XmlParser()\n" +
                "    def branches = parser.parse(file)\n\n" +
                "    def list = []\n\n" +
                "    branches.children().each {\n" +
                "        list.add(it.@releasebranchname)\n" +
                "    }\n\n" +
                "    return list"
    }

    private String removeBranchGroovy() {
        String shell =  "import groovy.xml.XmlUtil\n" +
                        "import jenkins.model.* \n" +
                        "def nodes_list = build.getEnvironment(listener).get('REMOVEABLE_BRANCHES').split(',')\n\n" +
                        "// Create a File object representing the file '<path><file>'\n" +
                        "def file = new File('" + PATH_TO_JOB_CONFIG + "', '" + projectName +"_release_branch_washingmachine')\n" +
                        "def instance = Jenkins.getInstance()\n\n" +
                        "// If it doesn't exist\n" +
                        "if ( !file.exists() ) {\n" +
                        "  return\n" +
                        "}\n\n" +
                        "XmlParser parser = new XmlParser()\n" +
                        "def branches = parser.parse(file)\n\n" +
                        "def nodes_to_be_removed = []\n" +
                        "branches.children().each {branch->\n" +
                        "  if (nodes_list.contains( branch.@releasebranchname ) ) {\n" +
                        "    nodes_to_be_removed.add(branch)\n" +
                        "    instance.getItem('" + projectName + "_washingmachine_releasebranch_' + branch.@releasebranchname).delete()\n" +
                        "    println('Deleted: " + projectName + "_washingmachine_releasebranch_' + branch.@releasebranchname)\n" +
                        "    instance.getItem('" + projectName + "_washingmachine_releasebranch_' + branch.@releasebranchname + '_onoff').delete()\n" +
                        "    println('Deleted: " + projectName + "_washingmachine_releasebranch_' + branch.@releasebranchname + '_onoff')\n" +
                        "    instance.getItem('" + projectName + "_washingmachine_releasebranch_' + branch.@releasebranchname + '_keepalive').delete()\n" +
                        "    println('Deleted: " + projectName + "_washingmachine_releasebranch_' + branch.@releasebranchname + '_keepalive')\n"
            if (useRpmWm) {
                shell +="    instance.getItem('" + projectName + "_washingmachine_releasebranch_rpm_' + branch.@releasebranchname).delete()\n" +
                        "    println('Deleted: " + projectName + "_washingmachine_releasebranch_rpm_' + branch.@releasebranchname)\n" +
                        "    instance.getItem('" + projectName + "_washingmachine_releasebranch_rpm_' + branch.@releasebranchname + '_onoff').delete()\n" +
                        "    println('Deleted: " + projectName + "_washingmachine_releasebranch_rpm_' + branch.@releasebranchname + '_onoff')\n" +
                        "    instance.getItem('" + projectName + "_washingmachine_releasebranch_rpm_' + branch.@releasebranchname + '_keepalive').delete()\n" +
                        "    println('Deleted: " + projectName + "_washingmachine_releasebranch_rpm_' + branch.@releasebranchname + '_keepalive')\n"
            }
            shell +=    "def deleteFile = new File('" + PATH_TO_JOB_CONFIG + "', '" + projectName +
                        "_washingmachine_releasebranch_' + branch.@releasebranchname + '_params.properties')\n" +
                        "    deleteFile.delete() \n" +
                        "    println('Deleted File: " + projectName + "_washingmachine_releasebranch_' + branch.@releasebranchname + '_params.properties')\n" +
                        "  }\n" +
                        "}\n\n"
          return shell
    }

    private String removeBranchBlameMail() {
        return "RELEASEBRANCHNAME = build.getEnvironment(listener).get('REMOVEABLE_BRANCHES')\n" +
        "\n" +
        "last_successful_wm_file = new File('/proj/eta-automation/blame_mail/', 'last_successful_wm_" + projectName + "_' + RELEASEBRANCHNAME + '.json')\n" +
        "\n" +
        "// If it exist remove it\n" +
        "if( last_successful_wm_file.exists() ) {\n" +
        "    boolean result = last_successful_wm_file.delete()\n" +
        "    if( !result){\n" +
        "        println('Failed To Deleted File: last_successful_wm_" + projectName + "_' + RELEASEBRANCHNAME + '.json' )\n" +
        "    } else{\n" +
        "        println('Deleted File: last_successful_wm_" + projectName + "_' + RELEASEBRANCHNAME + '.json' )\n" +
        "    }\n" +
        "}\n" +
        "\n" +
        "last_wm_file = new File('/proj/eta-automation/blame_mail/', 'last_wm_" + projectName + "_' + RELEASEBRANCHNAME + '.json')\n" +
        "\n" +
        "// If it exist remove it\n" +
        "if( last_wm_file.exists() ) {\n" +
        "    boolean result = last_wm_file.delete()\n" +
        "    if( !result){\n" +
        "        println('Failed To Deleted File: last_wm_" + projectName + "_' + RELEASEBRANCHNAME + '.json' )\n" +
        "    } else{\n" +
        "        println('Deleted File: last_wm_" + projectName + "_' + RELEASEBRANCHNAME + '.json' )\n" +
        "    }\n" +
        "}\n" +
        "\n" +
        "wm_blame_config_file = new File('/proj/eta-automation/blame_mail/', 'wm-blame-config-" + projectName + "-' + RELEASEBRANCHNAME + '.json')\n" +
        "\n" +
        "// If it exist remove it\n" +
        "if( wm_blame_config_file.exists() ) {\n" +
        "    boolean result = wm_blame_config_file.delete()\n" +
        "    if( !result){\n" +
        "        println('Failed To Deleted File: wm-blame-config-" + projectName + "-' + RELEASEBRANCHNAME + '.json' )\n" +
        "    } else{\n" +
        "        println('Deleted File: wm-blame-config-" + projectName + "-' + RELEASEBRANCHNAME + '.json' )\n" +
        "    }\n" +
        "}\n"
    }

    private String removeDefaultJiveSessionGroovy() {
        return "import groovy.json.JsonBuilder\n" +
                "import groovy.json.JsonSlurper\n" +
                "import groovy.xml.XmlUtil\n" +
                "import java.io.*\n" +
                "import java.net.*\n" +
                "import java.nio.charset.StandardCharsets\n" +
                "import java.util.*\n\n" +
                "def nodes_list = build.getEnvironment(listener).get('REMOVEABLE_BRANCHES').split(',')\n\n" +
                "// Create a File object representing the file '<path><file>'\n" +
                "def file = new File('" + PATH_TO_JOB_CONFIG + "', '" + projectName +"_release_branch_washingmachine')\n" +
                "// If it doesn't exist\n" +
                "if ( !file.exists() ) {\n" +
                "  return\n" +
                "}\n\n" +
                "String JIVECONTEXT = ''\n" +
                "String JIVESUITENAME = ''\n" +
                "XmlParser xmlParser = new XmlParser()\n" +
                "def branches = xmlParser.parse(file)\n\n" +
                "def nodes_to_be_removed = []\n" +
                "branches.children().each {branch->\n" +
                " println(branch.@releasebranchname)\n" +
                "  if (nodes_list.contains( branch.@releasebranchname ) ) {\n" +
                "    JIVECONTEXT = branch.@jivecontext\n" +
                "    JIVESUITENAME = branch.@jivesuitename\n" +
                "    nodes_to_be_removed.add(branch)\n" +
                "  }\n" +
                "}\n" +
                "nodes_to_be_removed.each{branch->\n" +
                "  branches.remove( branch )\n" +
                "}\n" +
                "def fw = new FileWriter( file )\n" +
                "fw.write(XmlUtil.serialize( branches ))\n" +
                "fw.close()\n\n" +
                "def url = new URL('https://jive.epk.ericsson.se/api/v1/projects/" + projectName + "/configuration')\n" +
                "def connection = url.openConnection()\n" +
                "connection.setRequestMethod('GET')\n" +
                "connection.connect()\n" +
                "def returnMessage = ''\n\n" +
                "if (connection.responseCode == 200 || connection.responseCode == 201){\n" +
                "   returnMessage = connection.content.text\n" +
                "   connection.disconnect();\n\n" +
                "   def parser = new JsonSlurper()\n" +
                "   def jsonResp = parser.parseText(returnMessage)\n" +
                "   HashMap<String, String> prodHashMap = new HashMap<String, String>();\n" +
                "   String defaultSessionGroup = '' + JIVECONTEXT + ' - ' + JIVESUITENAME + ' - Kascmadm'\n" +
                "   println(defaultSessionGroup)\n\n" +
                "   try {\n" +
                "       jsonResp.each { id, data ->\n" +
                "           if (id.equals('default_session_groups')) {\n" +
                "               data.eachWithIndex { item, index ->\n" +
                "                   if (data.text[index].equals(defaultSessionGroup)) {\n"+
                "                       println(jsonResp.default_session_groups.remove(data[index]))\n" +
                "                       throw new Exception('return from closure')\n" +
                "                   }\n" +
                "               }\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "   catch (Exception e) {\n" +
                "       println('Post request with JSON: ')\n" +
                "       println(jsonResp)\n" +
                "       def builder = new JsonBuilder(jsonResp)\n" +
                "       builder = builder.toString()\n" +
                "       println(builder)\n" +
                "       byte[] postData = builder.getBytes(StandardCharsets.UTF_8);\n" +
                "       int postDataLength = postData.length;\n" +
                "       String request = 'https://jive.epk.ericsson.se/api/v1/projects/" + projectName + "/configuration';\n" +
                "       URL urlPost = new URL(request);\n" +
                "       HttpURLConnection conn= (HttpURLConnection) urlPost.openConnection();\n" +
                "       conn.setDoOutput(true);\n" +
                "       conn.setRequestMethod('POST'); \n" +
                "       conn.setRequestProperty('Content-Type', 'application/json');\n" +
                "       conn.setRequestProperty('charset', 'utf-8');\n" +
                "       conn.setUseCaches(false);\n" +
                "       conn.setRequestProperty('Content-Length', Integer.toString(postDataLength));\n" +
                "       conn.connect();\n" +
                "       DataOutputStream wr = new DataOutputStream(conn.getOutputStream());\n" +
                "       wr.write(postData);\n" +
                "       wr.close();\n" +
                "       Reader isr = new BufferedReader(new InputStreamReader(conn.getInputStream(), 'utf-8'));\n" +
                "       for (int c; (c = isr.read()) >= 0;) {\n" +
                "           print((char)c);\n" +
                "       }\n" +
                "   }\n" +
                "} else {\n" +
                "   println('Error Connecting to ' + url)\n" +
                "}\n"
    }
}
