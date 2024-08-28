package com.ericsson.bss

import com.ericsson.bss.util.Email
import javaposse.jobdsl.dsl.Job

abstract class AbstractTapasJobBuilder extends AbstractJobBuilder {

    protected String tpgName = ""
    protected String suite
    protected String suiteFile
    protected String defaultTapasJobPath
    protected String tapasProjectName = ""
    protected String tapasProjectDestinationName = ""
    protected String variant
    protected String buildDescription = ""

    protected String jdkVersion = "Latest JDK 1.7"

    public void initProject(Job job) {
        super.initProject(job)
        if (tpgName.equals("")) {
            tpgName = projectName.split("\\.")[0]
        }
        if (tapasProjectName.equals("")) {
            tapasProjectName = tpgName
        }
        if (tapasProjectDestinationName.equals("")) {
            tapasProjectDestinationName = tpgName
        }
        super.configurePostBuildSteps().setBuildDescription()

        setUnstableOnTextFound('^.*Reporting end of session with status 2.*$', '')
        super.setJenkinsUserBuildVariables()
        setDescription() // Sets default description
        setInputParameters()
        setTapasShell()
        setConcurrentBuild()
        setJDKVersion()
    }

    protected void setDescription(String jobDescription = "") {
        job.with {
            description(DSL_DESCRIPTION + jobDescription)
        }
    }

    protected void setJDKVersion() {
        job.with {
            jdk(jdkVersion)
        }
    }

    protected void addSelectClusterParameter(String desc = "") {
        job.with {
            parameters {
                activeChoiceParam('CLUSTER') {
                    description(desc)
                    choiceType('SINGLE_SELECT')
                    groovyScript {
                        script(availableClusters())
                        fallbackScript(defaultFallbackScript())
                    }
                }
            }
        }
    }

    protected void addClusterReferenceParameter(String parameterName, String desc = "") {
        job.with {
            parameters {
                activeChoiceReactiveReferenceParam(parameterName) {
                    description(desc)
                    omitValueField()
                    choiceType('FORMATTED_HTML')
                    referencedParameter('CLUSTER')
                    groovyScript {
                        script(getClusterReferenceParameterScript('CLUSTER', parameterName.toLowerCase()))
                        fallbackScript(defaultFallbackScript())
                    }
                }
            }
        }
    }

    protected void addReferenceParameter(String parameterName, String refParameterName, String desc = "") {
        job.with {
            parameters {
                activeChoiceReactiveReferenceParam(parameterName) {
                    description(desc)
                    omitValueField()
                    choiceType('FORMATTED_HTML')
                    referencedParameter(refParameterName)
                    groovyScript {
                        script(getReferenceParameterScript(refParameterName))
                        fallbackScript(defaultFallbackScript())
                    }
                }
            }
        }
    }

    protected String availableClusters() {
        return "File file = new File('" + PATH_TO_JOB_CONFIG + "', '" +
               tpgName + "_cluster_list')\n" +
               "if( !file.exists() ) {\n" +
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
               "groovy.util.Node clusters = parser.parse(file)\n" +
               "ArrayList list = new ArrayList()\n" +
               "clusters.children().each {\n" +
               "    list.add(it.@name)\n" +
               "}\n" +
               "list.add(0,'MANUAL INPUT')\n" +
               "return list"
    }

    protected String getClusterReferenceParameterScript(String referenceParameter, String parameterName) {
        return "if (" + referenceParameter + ".equals('MANUAL INPUT')) {\n" +
               "    return \"<input name=\\\"value\\\" value=\\\"\\\" class=\\\"setting-input\\\" type=\\\"text"+
               "\\\">\"\n" +
               " } else {\n" +
               "    def file = new File('" + PATH_TO_JOB_CONFIG + "', '" + tpgName +
                "_cluster_list')\n" +
               "    if ( !file.exists() ) { return '' }\n" +
               "    def parser = new XmlParser()\n" +
               "    def clusters = parser.parse(file)\n" +
               "    def parameter_html = \"\"\n" +
               "    clusters.children().find {\n" +
               "        if ( " + referenceParameter + ".equals( it.@name ) ) {\n" +
               "            if (it.@" + parameterName + " && it.@" + parameterName + " != \"\") {\n" +
               "               parameter_html = \"<input name=\\\"value\\\" value=\\\"\" + it.@" + parameterName +
               " + \"\\\" class=\\\"setting-input\\\" type=\\\"text\\\" disabled>\"\n" +
               "            } else {\n" +
               "                parameter_html = \"<input name=\\\"value\\\" value=\\\"\\\" class=\\\"setting-input" +
               "\\\" type=\\\"text\\\">\"\n" +
               "            }\n" +
               "            return true\n" +
               "        }\n" +
               "        return false\n" +
               "    }\n" +
               "    return parameter_html\n" +
               "}\n"
    }

    protected String getReferenceParameterScript(String referenceParameter) {
        return "if (" + referenceParameter + ".equals('MANUAL INPUT')) {\n" +
               "    return \"<input name=\\\"value\\\" value=\\\"\\\" class=\\\"setting-input\\\" type=\\\"text"+
               "\\\">\"\n" +
               " } else {\n" +
               "   return \"<input name=\\\"value\\\" value=\\\"\" + " + referenceParameter + ".toString()+ \"\\\" class=\\\"setting-input\\\" type=\\\"text"+
               "\\\" disabled>\"\n" +
               "}\n"
    }

    protected void setInputParameters() {
    }

    protected void setUnstableOnTextFound(String textToFind, String description) {
        job.with {
            publishers {
                textFinder(textToFind, description, true, false, true)
            }
        }
    }

    protected void addSharpVersionChoiceParam(String parameterName, String metadataPath, String desc = "") {
        job.with {
            parameters {
                activeChoiceParam(parameterName) {
                    description(desc)
                    choiceType('SINGLE_SELECT')
                    groovyScript {
                        script("def version_list = []\n" +
                               "def metadata = new XmlSlurper().parse('" + metadataPath + "/maven-metadata.xml')\n" +
                               'metadata.versioning.versions.version.each{\n' +
                               '    version_list.add(it.text())\n' +
                               '}\n' +
                               '\n' +
                               'metadata = new XmlSlurper().parse("' + metadataPath + '/maven-metadata.xml")\n' +
                               'metadata.versioning.versions.version.each{\n' +
                                 "if (!version_list.contains(it.text().minus('-SNAPSHOT'))){\n" +
                                   'version_list.add(it.text())\n' +
                                 '}\n' +
                               '}\n' +
                               '\n' +
                               'version_list.sort{a,b-> b<=>a}\n' +
                               "version_list.add(0, 'LATEST');\n" +
                               '\n' +
                               'return version_list\n')
                        fallbackScript(defaultFallbackScript())
                    }
                }
            }
        }
    }

    protected void setTapasShell() {
        setExtraShell()
        job.with {
            steps {
                shell(symlinkMesosWorkSpace())
                shell(getTapasShell())
            }
        }
    }

    protected void setExtraShell() {
    }

    protected String getAdditionalEnvironmentVariables() {
        return ""
    }

    protected String getTapasParameters() {
        return ""
    }

    protected String getAdditionalTapasShell() {
        return ""
    }

    protected String getAdditionalFinishingTapasShell() {
        return ""
    }

    protected String getAdditionalMultipleHostShell() {
        return ""
    }

    protected String getAdditionalTapasAtExitShell() {
        return ""
    }

    protected String getJenkinsDescription() {
        return '    echo \"JENKINS_DESCRIPTION $BUILDDESCRIPTION <a href=\\\"$tapas_web_url\\\">Tapas session</a>\"\n'
    }

    protected String getTapasConfigSettings() {
        return 'BASE_CONFIG_FILE="/proj/eta-automation/tapas/config/' + tapasProjectName + '/' + suite + '"\n' +
               'CONFIG_FILE="/proj/eta-automation/tapas/sessions/' + tapasProjectDestinationName + '/' + suiteFile + '"\n'
    }

    protected String getTapasShell() {
        def shell = '' +
                    '#!/bin/bash\n' +
                    'umask 2\n' +
                    'export PYTHON_EGG_CACHE=$WORKSPACE\"/.python-eggs\"\n' +
                    'export PYTHONUNBUFFERED=1\n' +
                    'export PYTHONPATH=\"/proj/env/tapas\"\n' +
                    getAdditionalEnvironmentVariables() +
                    'set -e\n' +
                    'set +x\n' +
                    '\n' +
                    'export TAPAS_CONFIG=/proj/eta-automation/config/kascmadm/tapas_production.ini\n' +
                    'TAPAS_BASE=\"/proj/env/tapas/tapas/bin\"\n' +
                    '\n' +
                    'BUILDDESCRIPTION="' + buildDescription + '" \n' +
                    'trap "exit_actions" EXIT\n' +
                    'exit_actions(){\n' +
                    '    echo \"#############################################\"\n' +
                    '    echo \"############# Set tapas url #################\"\n' +
                    '    echo \"#############################################\"\n' +
                    '\n' +
                    '    # Find tapas url\n' +
                    '    tapas_url_cmd="grep \'Web url:\' $WORKSPACE/tapasconsole.txt | gawk -F\' \' \'{print \\\$6;exit;}\'\"\n' +
                    '    tapas_web_url=$(bash -c \"$tapas_url_cmd\")\n' +
                    '    if [ -z \"$tapas_web_url\" ]; then\n' +
                    '        tapas_web_url=\"https://tapas.epk.ericsson.se/#/suites/' + defaultTapasJobPath + '\"\n' +
                    '    fi\n' +
                    '\n' +
                    '    echo \"tapas_web_url: $tapas_web_url\"\n' +
                    '    echo \"tapas_web_url=$tapas_web_url\" >> env.properties\n' +
                    '\n' +
                    getAdditionalTapasAtExitShell() +
                    '\n' +
                    getJenkinsDescription() +
                    '\n' +
                    '    env | sort\n' +
                    '\n' +
                    '    # Make sure log tailing is killed if Tapas suite fails\n' +
                    '    sleep 5 \n' +
                    '    kill $logpid || echo "logpid $logpid already killed"\n' +
                    '\n' +
                    '}\n' +
                    '\n' +
                    getAdditionalMultipleHostShell() +
                    '\n' +
                    getTapasConfigSettings() +
                    '\n' +
                    getAdditionalTapasShell() +
                    '\n' +
                    'mkdir -p /proj/eta-automation/tapas/sessions/' + tapasProjectDestinationName + '\n' +
                    'cp $BASE_CONFIG_FILE $CONFIG_FILE\n' +
                    'cp $CONFIG_FILE tapas-config.xml\n' +
                    '\n' +
                    'if [[ ! -z ${TASKDIR} ]]; then\n' +
                    '    export TAPAS_OVERRIDE_TASKDIRS="${TASKDIR}"\n' +
                    'fi\n' +
                    '\n' +
                    'echo \"\"\n' +
                    'env | sort\n' +
                    'echo \"\"\n' +
                    '\n' +
                    'logfile=\"tapasconsole.txt\"\n' +
                    'test -f $logfile && rm $logfile\n' +
                    'touch $logfile\n' +
                    '\n' +
                    '${TAPAS_BASE}/tapas_runner.py -v -v \\\n' +
                    getTapasParameters() +
                    '-s ${CONFIG_FILE} >> ${logfile} 2>&1 &\n' +
                    'tapaspid=$!\n' +
                    '\n' +
                    'tail -f $logfile &\n' +
                    'logpid=$!\n' +
                    '\n' +
                    'wait $tapaspid\n' +
                    getAdditionalFinishingTapasShell()
            return shell
    }

    protected Email getAlwaysMailConfig()
    {
        return Email.newBuilder().withRecipient('$DEFAULT_RECIPIENTS')
                .withSubject('$DEFAULT_SUBJECT')
                .withContent('$DEFAULT_CONTENT')
                .withAlwaysTrigger()
                .build()
    }

    protected List getNumberedList(int start, int number) {
        List numberedList = []
        for (int i = start; i < start + number; i++){
            numberedList << (i).toString()
        }
        return numberedList
    }
}
