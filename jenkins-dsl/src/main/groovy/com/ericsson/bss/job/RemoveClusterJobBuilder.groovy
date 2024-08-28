package com.ericsson.bss.job

import com.ericsson.bss.AbstractGerritJobBuilder

import javaposse.jobdsl.dsl.Job

public class RemoveClusterJobBuilder extends AbstractGerritJobBuilder {

    private String projectName

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addRemoveClusterConfig()
        return job
    }

    public void addRemoveClusterConfig() {
        job.with {

            String jobDescription = "" +
                    "<h2>Remove a group of machines for the " + projectName + " cluster list.</h2>"

            description(DSL_DESCRIPTION + jobDescription)
            concurrentBuild()
            parameters {
                activeChoiceParam('REMOVEABLE_CLUSTERS') {
                    description('Choose cluster to remove')
                    filterable()
                    choiceType('CHECKBOX')
                    groovyScript {
                        script(removableCluster())
                        fallbackScript('return ["Error evaluating Groovy script."]')
                    }
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
                systemGroovyCommand(removeClusterGroovy())
            }
        }
    }

    private String removableCluster() {
        return "" +
                "    // Create a File object representing the file '<path><file>'\n" +
                "    def file = new File('" + PATH_TO_JOB_CONFIG + "', '" + projectName + "_cluster_list')\n" +
                "\n" +
                "// If it doesn't exist\n" +
                "    if ( !file.exists() ) {\n" +
                "        return \"\"\n" +
                "    }\n" +
                "\n" +
                "    def parser = new XmlParser()\n" +
                "    def clusters = parser.parse(file)\n" +
                "\n" +
                "    def list = []\n" +
                "\n" +
                "    clusters.children().each {\n" +
                "        list.add(it.@name)\n" +
                "    }\n" +
                "\n" +
                "    return list"
    }

    private String availableGroups() {
        return "" +
                "import groovy.xml.MarkupBuilder\n" +
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
    private String removeClusterGroovy() {
        return "" +
                "import groovy.xml.XmlUtil\n" +
                "\n" +
                "def nodes_list = build.getEnvironment(listener).get('REMOVEABLE_CLUSTERS').split(',')\n" +
                "\n" +
                "// Create a File object representing the file '<path><file>'\n" +
                "def file = new File('" + PATH_TO_JOB_CONFIG + "', '" + projectName +"_cluster_list')\n" +
                "\n" +
                "// If it doesn't exist\n" +
                "if ( !file.exists() ) {\n" +
                "  return\n" +
                "}\n" +
                "\n" +
                "XmlParser parser = new XmlParser()\n" +
                "def clusters = parser.parse(file)\n" +
                "\n" +
                "def nodes_to_be_removed = []\n" +
                "clusters.children().each {cluster->\n" +
                "  if (nodes_list.contains( cluster.@name ) ) {\n" +
                "    nodes_to_be_removed.add(cluster)\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "nodes_to_be_removed.each{cluster->\n" +
                "  clusters.remove( cluster )\n" +
                "}\n" +
                "\n" +
                "def fw = new FileWriter( file )\n" +
                "fw.write(XmlUtil.serialize( clusters ))\n" +
                "fw.close()"
    }
}
