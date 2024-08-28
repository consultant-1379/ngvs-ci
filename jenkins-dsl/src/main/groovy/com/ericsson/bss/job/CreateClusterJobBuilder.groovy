package com.ericsson.bss.job

import com.ericsson.bss.AbstractGerritJobBuilder

import javaposse.jobdsl.dsl.Job

public class CreateClusterJobBuilder extends AbstractGerritJobBuilder {

    private String projectName
    protected boolean useTwoTargethosts = false
    protected List tpgSpecificMachines = []
    protected boolean useCil = true

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addCreateClusterConfig()
        return job
    }

    public void addCreateClusterConfig() {
        job.with {

            String jobDescription = "" +
                    "<h2>Create a group of machines for the " + projectName + " cluster list.</h2>"

            description(DSL_DESCRIPTION + jobDescription)
            concurrentBuild()

            parameters {
                stringParam('CLUSTER_GROUP_NAME', '', 'Name your group')
                stringParam('TARGETHOST', '', 'Targethost to be used')
                if (useTwoTargethosts) {
                    stringParam('TARGETHOST2', '', 'Targethost2 to be used')
                }
                stringParam('MSV', '', 'MSV to be used')
                if (useCil) {
                    stringParam('CIL', '', 'CIL to be used')
                }
                stringParam('INSTALLNODE', '', '<b>[Optional]</b> Installnode to be used')
                tpgSpecificMachines.each {
                    stringParam(it, '', "${it} to be used")
                }
                activeChoiceReactiveReferenceParam('AVAILABLE_GROUPS') {
                    description('List existing groups')
                    omitValueField()
                    choiceType('FORMATTED_HTML')
                    groovyScript {
                        script(availableGroups())
                        fallbackScript('return ["Error evaluating Groovy script."]')
                    }
                }
            }

            steps {
                systemGroovyCommand(createClusterGroovy())
            }
        }
    }

    private String availableGroups() {
        return "import groovy.xml.MarkupBuilder\n" +
               "\n" +
               "// Create a File object representing the file '<path><file>'\n" +
               "def file = new File('" + PATH_TO_JOB_CONFIG + "', '" + projectName +"_cluster_list')\n" +
               "\n" +
               "// If it doesn't exist\n" +
               "if ( !file.exists() ) {\n" +
               "  return \"\"\n" +
               "}\n" +
               "\n" +
               "def parser = new XmlParser()\n" +
               "def clusters = parser.parse(file)\n" +
               "\n" +
               "def writer = new StringWriter()\n" +
               "MarkupBuilder builder = new MarkupBuilder(writer)\n" +
               "builder.span {\n" +
               "  clusters.children().each {cluster->\n" +
               "    ul() {\n" +
               "      cluster.attributes().each{k, v ->\n" +
               "        li(k + \": \" + v)\n" +
               "      }\n" +
               "    }\n" +
               "  }\n" +
               "}\n" +
               "\n" +
               "return writer.toString()"
    }

    private String createClusterGroovy() {
        String ret = "import groovy.xml.XmlUtil\n" +
               "\n" +
               "String CGN = build.getEnvironment(listener).get('CLUSTER_GROUP_NAME')\n" +
               "String TH = build.getEnvironment(listener).get('TARGETHOST')\n"
               if (useCil) {
                   ret += "String CIL = build.getEnvironment(listener).get('CIL')\n"
               }
               ret += "String MSV = build.getEnvironment(listener).get('MSV')\n" +
               "String INSTALLNODE = build.getEnvironment(listener).get('INSTALLNODE')\n" +
               getExtraTargethostparameter() +
               getTpgSpecificParameters() +
               "\n" +
               "if (INSTALLNODE.equals( \"\")){\n" +
               "  INSTALLNODE = \"\"\n" +
               "}\n" +
               "\n" +
               "// Create a File object representing the file '<path><file>'\n" +
               "def file = new File('" + PATH_TO_JOB_CONFIG + "', '" + projectName +"_cluster_list')\n" +
               "\n" +
               "// If it doesn't exist\n" +
               "if ( !file.exists() ) {\n" +
               "  // Create file\n" +
               "  file.createNewFile()\n" +
               "  def sw = new StringWriter()\n" +
               "  def xml = new groovy.xml.MarkupBuilder(sw)\n" +
               "  xml.clusters(){\n" +
               "     cluster(name: CGN, targethost: TH," + getXmlExtraTargethost()
               if (useCil) {
                   ret += "cil: CIL, "
               }
               ret += "msv: MSV, " + getTpgSpecificXml() + " installnode: INSTALLNODE)\n" +
               "  }\n" +
               "  def fw = new FileWriter(file)\n" +
               "  fw.write(sw.toString())\n" +
               "  fw.close()\n" +
               "  return\n" +
               "}\n" +
               "\n" +
               "XmlParser parser = new XmlParser()\n" +
               "def clusters = parser.parse(file)\n" +
               "\n" +
               "parser.createNode(\n" +
               "  clusters,\n" +
               "  \"cluster\",\n" +
               "  [name: CGN, targethost: TH," + getXmlExtraTargethost()
               if (useCil) {
                   ret += " cil: CIL,"
               }
               ret += " msv: MSV, " +
               getTpgSpecificXml() + " installnode: INSTALLNODE]\n" +
               ")\n" +
               "\n" +
               "def fw = new FileWriter(file)\n" +
               "fw.write(XmlUtil.serialize(clusters))\n" +
               "fw.close()"
        return ret
    }

    private String getExtraTargethostparameter() {
        if (useTwoTargethosts) {
            return "String TH2 = build.getEnvironment(listener).get('TARGETHOST2')\n"
        }
        return ""
    }

    private String getTpgSpecificParameters() {
        String parameter = ""
        tpgSpecificMachines.each {
            parameter += "String ${it} = build.getEnvironment(listener).get('${it}')\n"
        }
        return parameter
    }

    private String getXmlExtraTargethost() {
        if (useTwoTargethosts){
            return " targethost2: TH2,"
        }
        return ""
    }

    private String getTpgSpecificXml() {
        String parameter = ""
        tpgSpecificMachines.each {
            parameter += " ${it.toLowerCase()}: ${it},"
        }
        return parameter
    }
}
