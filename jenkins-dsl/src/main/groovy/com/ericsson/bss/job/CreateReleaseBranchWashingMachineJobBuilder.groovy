package com.ericsson.bss.job

import com.ericsson.bss.AbstractGerritJobBuilder

import javaposse.jobdsl.dsl.Job

public class CreateReleaseBranchWashingMachineJobBuilder extends AbstractGerritJobBuilder {

    protected String projectName
    protected boolean useTwoTargethosts = false
    protected boolean useGitBranch = false

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addCreateClusterConfig()
        return job
    }

    public void addCreateClusterConfig() {
        job.with {

            String jobDescription = "<h2>Create a Washingmachine " + projectName + " on a release branch.</h2>"

            description(DSL_DESCRIPTION + jobDescription)
            label(RESTRICT_LABEL_MESOS)
            concurrentBuild()
            parameters {

                stringParam('TARGETHOST', '', 'Targethost to be used')
                if (useTwoTargethosts) {
                    stringParam('TARGETHOST2', '', 'Targethost2 to be used')
                }
                stringParam('MSV', '', 'MSV to be used')
                stringParam('CIL', '', 'CIL to be used')
                stringParam('INSTALLNODE', '', 'Installnode to be used')
                stringParam('VERSION', '', 'Release branch version e.g. x.y-LATEST')
                if (useTwoTargethosts) {
                    stringParam('VERSION2', '', 'Release branch version e.g. x.y-LATEST')
                }
                stringParam('RELEASEBRANCHNAME', '', 'Releasebranch dropname, e.g 1620, <b>DO NOT USE SPACES</b>')
                stringParam('JIVEVERSION', '', 'Jive version to download')
                stringParam('JIVECONTEXT', '', 'Jive context to use in jive suite')
                stringParam('JIVESUITENAME', '', 'Jive SuiteName that is used in jive suite, this is to get correct suite to default sessions <br>' +
                        'Use NoName if no jivesuitename exists')
                stringParam('RPMVERSION', '', 'Release branch RPM version e.g. x.y-LATEST')
                if (useTwoTargethosts) {
                    stringParam('RPMVERSION2', '', 'Release branch RPM version e.g. x.y-LATEST')
                }
                stringParam('RUNNFTTEST', 'yes', 'Option to enable or disable NFT test if the suite uses it.' +
                                                 '"yes" to have it enabled and "no to dissable it"')
                if (useGitBranch) {
                    stringParam('GITBRANCH', '', 'Name of the JIVE Git Patch, e.g release/1.8')
                }
            }

            steps {
                shell(createJiveContext())
                systemGroovyCommand(createClusterGroovy())
                systemGroovyCommand(addDefaultJiveSessionGroovy())
                systemGroovyCommand(createBlameFilesGroovy())
            }
            publishers {
                postBuildScripts {
                    steps {
                        downstreamParameterized {
                            trigger(projectName + '_dsl') {
                                block {
                                    buildStepFailure('FAILURE')
                                    failure('FAILURE')
                                    unstable('UNSTABLE')
                                }
                            }
                        }
                        onlyIfBuildSucceeds(true)
                    }
                }

                postBuildScripts {
                    steps {
                        conditionalSteps {
                            condition {
                                status('SUCCESS', 'SUCCESS')
                            }
                            steps {
                                downstreamParameterized {
                                    trigger(projectName + '_washingmachine_releasebranch_\${RELEASEBRANCHNAME}')
                                }
                            }
                        }
                    }
                }

            }
        }
        getExtraOptions()
    }

    protected String createClusterGroovy() {
        String returnValue = "import groovy.xml.XmlUtil\n\n" +
                             "String TH = build.getEnvironment(listener).get('TARGETHOST')\n" +
                             "String CIL = build.getEnvironment(listener).get('CIL')\n" +
                             "String MSV = build.getEnvironment(listener).get('MSV')\n" +
                             "String INSTALLNODE = build.getEnvironment(listener).get('INSTALLNODE')\n" +
                             "String VERSION = build.getEnvironment(listener).get('VERSION')\n" +
                             "String RELEASEBRANCHNAME = build.getEnvironment(listener).get('RELEASEBRANCHNAME')\n" +
                             "String JIVEVERSION = build.getEnvironment(listener).get('JIVEVERSION')\n" +
                             "String JIVECONTEXT = build.getEnvironment(listener).get('JIVECONTEXT')\n" +
                             "String JIVESUITENAME = build.getEnvironment(listener).get('JIVESUITENAME')\n" +
                             "String RPMVERSION = build.getEnvironment(listener).get('RPMVERSION')\n" +
                             "String RUNNFTTEST = build.getEnvironment(listener).get('RUNNFTTEST')\n"
        if (useGitBranch) {
            returnValue += "String GITBRANCH = build.getEnvironment(listener).get('GITBRANCH')\n"
        }
        returnValue += getExtraTargethostparameter() +
                       getExtraParameters() +
                       "\n" +
                       "if (INSTALLNODE.equals( \"\")){\n" +
                       "  INSTALLNODE = \"\"\n" +
                       "}\n\n" +
                       "// Create a File object representing the file '<path><file>'\n" +
                       "def file = new File('" + PATH_TO_JOB_CONFIG + "', '" + projectName +"_release_branch_washingmachine')\n\n" +
                       "// If it doesn't exist\n" +
                       "if ( !file.exists() ) {\n" +
                       "  // Create file\n" +
                       "  file.createNewFile()\n" +
                       "  def sw = new StringWriter()\n" +
                       "  def xml = new groovy.xml.MarkupBuilder(sw)\n" +
                       "  xml.clusters(){\n" +
                       "     cluster(targethost: TH," + getXmlExtraTargethost() +
                       " cil: CIL, msv: MSV, installnode: INSTALLNODE, version: VERSION, releasebranchname: RELEASEBRANCHNAME, jiveversion: JIVEVERSION, " +
                       "jivecontext: JIVECONTEXT, jivesuitename: JIVESUITENAME, rpmversion: RPMVERSION, " +
                        "runnfttest: RUNNFTTEST"
        if (useGitBranch) {
            returnValue += ", gitbranch: GITBRANCH"
        }
        returnValue += getXmlExtraParameters() +")\n" +
                       "  }\n" +
                       "  def fw = new FileWriter(file)\n" +
                       "  fw.write(sw.toString())\n" +
                       "  fw.close()\n" +
                       "  return\n" +
                       "}\n\n" +
                       "XmlParser parser = new XmlParser()\n" +
                       "def clusters = parser.parse(file)\n\n" +
                       "parser.createNode(\n" +
                       "  clusters,\n" +
                       "  \"cluster\",\n" +
                       "  [targethost: TH," + getXmlExtraTargethost() +
                       " cil: CIL, msv: MSV, installnode: INSTALLNODE, version: VERSION, releasebranchname: RELEASEBRANCHNAME, jiveversion: JIVEVERSION, " +
                       "jivecontext: JIVECONTEXT, jivesuitename: JIVESUITENAME, rpmversion: RPMVERSION, runnfttest: RUNNFTTEST"
        if (useGitBranch) {
            returnValue += ", gitbranch: GITBRANCH"
        }
        returnValue += getXmlExtraParameters() + "]\n" +
                       ")\n\n" +
                       "def fw = new FileWriter(file)\n" +
                       "fw.write(XmlUtil.serialize(clusters))\n" +
                       "fw.close()"
        return returnValue
    }

    protected String createBlameFilesGroovy() {
        return "RELEASEBRANCHNAME = build.getEnvironment(listener).get('RELEASEBRANCHNAME')\n" +
        "\n" +
        "last_successful_wm_file_name = '/proj/eta-automation/blame_mail/last_successful_wm_" + projectName + "_' + RELEASEBRANCHNAME + '.json'\n" +
        "last_wm_file_name = '/proj/eta-automation/blame_mail/last_wm_" + projectName + "_' + RELEASEBRANCHNAME + '.json'\n" +
        "\n" +
        'wm_blame_config_file = new File("/proj/eta-automation/blame_mail/", "wm-blame-config-' + projectName + '-" + RELEASEBRANCHNAME + ".json")\n' +
        'wm_blame_config_template = new File("/proj/eta-automation/tapas/config/' + projectName + '/config/blame/", "blame-config-template.json")\n' +
        "\n" +
        "private copyAndReplaceText(source, dest, Closure replaceText){\n" +
        "  dest.write(replaceText(source.text))\n" +
        "}\n" +
        "\n" +
        "if(!wm_blame_config_file.exists()){\n" +
        "  wm_blame_config_file.createNewFile()\n" +
        "\n" +
        "  copyAndReplaceText(wm_blame_config_template, wm_blame_config_file){\n" +
        "      it.replace('\${LAST_SUCCESSFUL_RUN_FILE}', last_successful_wm_file_name)" +
        ".replace('\${LAST_RUN_FILE}', last_wm_file_name)" +
        ".replace('\${WM_NAME_SUFFIX}', ' ' + RELEASEBRANCHNAME)\n" +
        "  }\n" +
        "}\n"
    }

    protected String addDefaultJiveSessionGroovy() {
        return "import groovy.json.JsonBuilder\n" +
                "import groovy.json.JsonSlurper\n" +
                "import java.io.*\n" +
                "import java.net.*\n" +
                "import java.nio.charset.StandardCharsets\n" +
                "import java.util.*\n\n" +
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
                "   def JIVECONTEXT = build.getEnvironment(listener).get('JIVECONTEXT')\n" +
                "   def JIVESUITENAME = build.getEnvironment(listener).get('JIVESUITENAME')\n" +
                "   HashMap<String, String> prodHashMap = new HashMap<String, String>();\n" +
                "   String defaultSessionGroup = '' + JIVECONTEXT + ' - ' + JIVESUITENAME + ' - Kascmadm'\n" +
                "   prodHashMap.put('text', defaultSessionGroup);\n\n" +
                "   try {\n" +
                "       jsonResp.each { id, data ->\n" +
                "           if (id.equals('default_session_groups')) {\n" +
                "               data.eachWithIndex { item, index ->\n" +
                "                   println(jsonResp.default_session_groups.add(prodHashMap))\n" +
                "                   throw new Exception('return from closure')\n" +
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

    protected String createJiveContext() {
        return "/proj/eta-tools/gnu-bundle/linux/production/bin/curl -X POST -d '{\"name\":\"'\${JIVECONTEXT}'\",\"description\":\"The context used by " +
                projectName + " Washingmachine on releasebranch '\${RELEASEBRANCHNAME}'\"}' \"https://jive.epk.ericsson.se/api/v1/projects/" + projectName +
                "/contexts\" --header \"Content-Type:application/json\""
    }

    protected String getExtraTargethostparameter() {
        if (useTwoTargethosts) {
            return "def TH2 = build.getEnvironment(listener).get('TARGETHOST2')\n" +
                    "def VERSION2 = build.getEnvironment(listener).get('VERSION2')\n" +
                    "def RPMVERSION2 = build.getEnvironment(listener).get('RPMVERSION2')\n"
        }
        return ""
    }

    protected String getXmlExtraTargethost() {
        if (useTwoTargethosts){
            return " targethost2: TH2, version2: VERSION2, rpmversion2: RPMVERSION2, "
        }
        return ""
    }

    protected void getExtraOptions() {
        return
    }

    protected String getExtraParameters() {
        return ""
    }

    protected String getXmlExtraParameters() {
        return ""
    }

}
