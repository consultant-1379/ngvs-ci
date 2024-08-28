package com.ericsson.bss.job.rmca

import com.ericsson.bss.job.AddRemoveNightlyFullTargethostInstallJobBuilder

public class RmcaAddRemoveNightlyFullTargethostInstallJobBuilder extends AddRemoveNightlyFullTargethostInstallJobBuilder {

    @Override
    protected void getOptionalInputParameters() {
        job.with {
            parameters {
                activeChoiceReactiveReferenceParam('RUNSIMULATORS') {
                    choiceType('FORMATTED_HTML')
                    omitValueField()
                    groovyScript {
                        script(getRunSimulatorsScript())
                        fallbackScript(defaultFallbackScript())
                    }
                    description("If the job should start simulators for CHA, COBA and NTF.")
                    referencedParameter('ADDCLUSTER,REMOVECLUSTER')
                }
            }
        }
    }

    private String getRunSimulatorsScript() {
        return "if(ADDCLUSTER.equals(\"INPUT STRING\")) {\n" +
               "    if(ADDCLUSTER.equals(\"INPUT STRING\"))\n" +
               "    {\n" +
               "        return \"<input name=\\\"value\\\" value=\\\"true\\\" " +
                                "type=\\\"checkbox\\\">\"\n" +
               "    } \n" +
               "    else {\n" +
               "        // Create a File object representing the file '<path><file>'\n" +
               "        File file = new File('" + PATH_TO_JOB_CONFIG + "', '" +
                                            tpgName + "_nightly_list')\n" +
               "\n" +
               "        // If it doesn't exist\n" +
               "        if( !file.exists() ) {\n" +
               "            return \"\"\n" +
               "        }\n" +
               "\n" +
               "        XmlParser parser = new XmlParser()\n" +
               "        groovy.util.Node clusters = parser.parse(file)\n" +
               "\n" +
               "        String runsimulators_html = \"\"\n" +
               "\n" +
               "        clusters.children().find {\n" +
               "            if( REMOVECLUSTER.equals( it.@name ) ) {\n" +
               "                String runsimulators = it.@runsimulators\n" +
               "                if( !runsimulators ) {\n" +
               "                    runsimulators = \"false\"\n" +
               "                }\n" +
               "                runsimulators_html = \"<input name=\\\"value\\\" value=\\\"\" " +
                                                      "+ runsimulators + \"\\\" " +
                                                      "class=\\\"setting-input\\\" " +
                                                      "type=\\\"text\\\" disabled>\"\n" +
               "                return true\n" +
               "            }\n" +
               "            return false\n" +
               "        }\n" +
               "        return runsimulators_html\n" +
               "    }\n" +
               "}\n" +
               "else {\n" +
               "    // Create a File object representing the file '<path><file>'\n" +
               "    File file = new File('" + PATH_TO_JOB_CONFIG + "', '" + tpgName +
                                        "_cluster_list')\n" +
               "\n" +
               "    // If it doesn't exist\n" +
               "    if( !file.exists() ) {\n" +
               "        return \"\"\n" +
               "    }\n" +
               "\n" +
               "    XmlParser parser = new XmlParser()\n" +
               "    groovy.util.Node clusters = parser.parse(file)\n" +
               "\n" +
               "    String runsimulators_html = \"\"\n" +
               "\n" +
               "    clusters.children().find {\n" +
               "        if( ADDCLUSTER.equals( it.@name ) &&\n" +
               "            REMOVECLUSTER.equals(\"INPUT STRING\") ) {\n" +
               "            String runsimulators = it.@runsimulators\n" +
               "            if( !runsimulators ) {\n" +
               "                runsimulators = \"false\"\n" +
               "            }\n" +
               "            runsimulators_html = \"<input name=\\\"value\\\" value=\\\"\" + " +
                                                  "runsimulators + \"\\\" " +
                                                  "class=\\\"setting-input\\\" type=\\\"text\\\"" +
                                                  ">\"\n" +
               "            return true\n" +
               "        }\n" +
               "        return false\n" +
               "    }\n" +
               "    return runsimulators_html\n" +
               "}"
    }

    protected String getTpgSpecificParameters() {
        return "String RUNSIMULATORS = paramAction.getParameter('RUNSIMULATORS').getValue()\n"
    }

    protected String getTpgSpecificProperties() {
        return ", runsimulators:RUNSIMULATORS"
    }

}