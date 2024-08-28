package com.ericsson.bss.job

import com.ericsson.bss.AbstractTapasJobBuilder
import javaposse.jobdsl.dsl.Job

public class SetMsvCilJobBuilder extends AbstractTapasJobBuilder {

    protected String jenkinsURL = ""

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        setDescription()
        setConcurrentBuild(false)
        discardOldBuilds(30, 30)
        deleteWorkspaceBeforeBuildStarts()
        addTimeoutAndAbortConfig(240)
        addBuildParameters()
        deleteWorkspaceAfterBuild()
        return job
    }

    @Override
    protected void setDescription() {
        String targethostInstall = jenkinsURL + "job/" + tpgName + "_nightly_targethost_install/"
        String washingmachineUpgrade = jenkinsURL + "job/" + tpgName + "_nightly_washingmachine_msv_cil_upgrade"

        job.with {
            description(DSL_DESCRIPTION +
                '<h2>This specified the MSV and CIL version a cluster(s) should be upgraded to.</h2>' +
                'The MSV and CIL versions are used by <a href ="' +
                targethostInstall + '" />' + tpgName + '_nightly_targethost_install</a> and ' +
                '<a href ="' + washingmachineUpgrade + '" />' + tpgName +
                '_nightly_washingmachine_msv_cil_upgrade</a>.<br> For the nightly_targethost_install job the ' +
                '"master" branch should be selected.')
        }
    }

    protected void addBuildParameters() {
        job.with {
            parameters {
                activeChoiceParam('BRANCH') {
                    groovyScript {
                        script(getBranchScript())
                        fallbackScript(defaultFallbackScript())
                    }
                    description("Select which branch's MSV and CIL version should be changed.<br>" +
                                "For targethost clusters 'Master' should be selected.")
                }
                activeChoiceParam('MSVVERSION') {
                    groovyScript {
                        script(getMsvVersionsScript())
                        fallbackScript(defaultFallbackScript())
                    }
                    description("Which MSV the branch should install")
                }
                activeChoiceParam('CILVERSION') {
                    groovyScript {
                        script(getCilVersionsScript())
                        fallbackScript(defaultFallbackScript())
                    }
                    description("Which CIL the branch should install")
                }
                activeChoiceReactiveReferenceParam('CURRENT_VERSIONS') {
                    choiceType('FORMATTED_HTML')
                    omitValueField()
                    groovyScript {
                        script(getCurrentVersions())
                        fallbackScript(defaultFallbackScript())
                    }
                    referencedParameter('BRANCH')
                }
            }
        }
    }

    @Override
    protected void setTapasShell() {
        setExtraShell()
        job.with {
            steps {
                shell(getBuildScript())
            }
        }
    }

    @Override
    protected void archiveArtifacts() {
    }

    private String getBranchScript() {
        return "ArrayList version_list = new ArrayList()\n" +
               "File file = new File('" + PATH_TO_JOB_CONFIG + "', '" + tpgName +
                                     "_branches_list')\n" +
               "if( !file.exists() ) {\n" +
               "    return \"\"\n" +
               "}\n" +
               "XmlParser parser = new XmlParser()\n" +
               "groovy.util.Node versions = parser.parse(file)\n" +
               "versions.children().each {\n" +
               "    version_list.add(it.@branch)\n" +
               "}\n" +
               "file = new File('" + PATH_TO_JOB_CONFIG + "', '" + tpgName +
                                "_release_branch_washingmachine')\n" +
               "if (!file.exists()) {\n" +
               "    return version_list\n" +
               "}\n" +
               "versions = parser.parse(file)\n" +
               "versions.children().each {\n" +
               "    version_list.add('releasebranch_' + it.@releasebranchname)\n" +
               "}\n" +
               "return version_list"
    }

    private String getMsvVersionsScript() {
        return "ArrayList version_list = new ArrayList()\n" +
               "File file = new File('" + PATH_TO_JOB_CONFIG + "', " +
                                     "'msv_version_list')\n" +
               "if( !file.exists() ) {\n" +
               "  return \"\"\n" +
               "}\n" +
               "XmlParser parser = new XmlParser()\n" +
               "groovy.util.Node versions = parser.parse(file)\n" +
               "versions.children().each {\n" +
               "  version_list.add(it.@ver)\n" +
               "}\n" +
               "return version_list"
    }

    private String getCilVersionsScript() {
        return """            /**/
            version_list = []
            metadataPath = "https://arm.epk.ericsson.se/artifactory/proj-cil-release-local/com/ericsson/bss/cil/server/cil-server-dv," +
                           "https://arm.epk.ericsson.se/artifactory/proj-cil-release-local/com/ericsson/bss/cil/server/cil-staging/"

            try {
                files = metadataPath.tokenize(',')
                for (String file : files) {
                    (path, fromversion) = file.tokenize(';')
                    metadata = new XmlSlurper().parse(path + '/maven-metadata.xml')
                    metadata.versioning.versions.version.each{
                        if (!fromversion || includeVersion(fromversion, it.text())) {
                            version_list.add(it.text())
                        }
                    }
                }
            } catch (IOException e) {}

            version_list.sort{a, b-> a == b ? 0 : includeVersion(b, a) ? -1 : 1}

            return version_list

            /*
             * The sorting algorith will first run a 2 part tokenizer
             * splitting away anything after a "-" for a string compare
             * and every "." for an integral compare.
             *
             * Each version part (major, minor, micro) is compared in
             * order and returns an early result on missmatch.
             * If all version parts are equal we run the string compare
             * if available (both version strings contain a "-") and
             * return the result.
             *
            */
            boolean includeVersion(String limitVer, String requestVer) {
                limitrcpart = ""
                requestrcpart = ""
                limitParts = ""
                requestParts = ""

                if (limitVer.contains("-")) {
                    limitrcparts = limitVer.tokenize("-")
                    limitrcpart = limitrcparts[1]
                    limitParts = limitrcparts[0].tokenize(".")
                } else {
                    limitParts = limitVer.tokenize(".")
                }

                if (requestVer.contains("-")) {
                    requestrcparts = requestVer.tokenize("-")
                    requestrcpart = requestrcparts[1]
                    requestParts = requestrcparts[0].tokenize(".")
                } else {
                    requestParts = requestVer.tokenize(".")
                }

                for (int i = 0; i < 3; i++) {
                    try {
                        limitNum = limitParts[i].toInteger()
                        requestNum = requestParts[i].toInteger()

                        if (requestNum < limitNum) {
                            return false
                        }
                        else if (requestNum > limitNum) {
                            return true
                        }
                    }
                    catch (Exception e) {
                        return false
                    }
                }
                /*
                 * Make sure versions without strings get placed above those with.
                 * ex. 2.2.0 is "larger" than 2.2.0-rcx.
                */
                if (limitrcpart.equals("")) {
                    return false
                }
                if (requestrcpart.equals("")) {
                    return true
                }
                /*
                 * At this point we know both versions contains string
                 * So we do an lexicographical string compare to determine
                 * the "greater" string.
                */
                return requestrcpart.compareTo(limitrcpart) > 0
            }
        """.stripIndent()
    }

    private String getCurrentVersions() {
        return "import groovy.xml.MarkupBuilder\n" +
                "\n" +
                "StringWriter writer = new StringWriter()\n" +
                "XmlParser parser = new XmlParser()\n" +
                "MarkupBuilder builder = new MarkupBuilder(writer)\n" +
                "\n" +
                "String branch = ''\n" +
                "if (BRANCH != 'master') {\n" +
                "    branch = BRANCH + '_'\n" +
                "}\n" +
                "\n" +
                "\n" +
                "File file = new File('" + PATH_TO_JOB_CONFIG + "', '" +
                                      tpgName + "_washingmachine_' + branch + 'params.properties')\n" +
                "if (!file.exists()) {\n" +
                "    return '" + tpgName + "_washingmachine_' + branch + 'params.properties'\n" +
                "}\n" +
                "\n" +
                "builder.span {\n" +
                "    ul() {\n" +
                "        file.eachLine { line ->\n" +
                "            String regexPattern = /[CIL|MSV]_VERSION.*/\n" +
                "            java.util.regex.Matcher result = (line =~ regexPattern)\n" +
                "            if (!result) {\n" +
                "                return\n" +
                "            }\n" +
                "            li(line)\n" +
                "        }\n" +
                "        }\n" +
                "}\n" +
                "return writer.toString()"
    }

    private String getBuildScript() {
        return "#!/bin/bash\n" +
               "\n" +
               "if [[ \$BRANCH != \"master\" ]]; then\n" +
               "    BRANCH=\$BRANCH\"_\"\n" +
               "else\n" +
               "    BRANCH=\"\"\n" +
               "fi\n" +
               "\n" +
               "PROP_FILE=`cat " + PATH_TO_JOB_CONFIG + tpgName +
                               "_washingmachine_\${BRANCH}params.properties`\n" +
               "\n" +
               "msv_regex=\"(MSV_VERSION=)([0-9]*\\.[0-9]*\\.[0-9]*(\\-SNAPSHOT)?)\"\n" +
               "cil_regex=\"(CIL_VERSION=)([0-9]*\\.[0-9]*\\.[0-9]*(\\-SNAPSHOT)?)\"\n" +
               "\n" +
               "if [[ \$PROP_FILE =~ \$msv_regex ]]; then\n" +
               "    PROP_FILE=`echo \"\$PROP_FILE\" | sed -r \"s/\$msv_regex/\\1\$MSVVERSION/\"`\n"+
               "else\n" +
               "    PROP_FILE=`echo -e \"\$PROP_FILE\"\"\\nMSV_VERSION=\${MSVVERSION}\"`\n" +
               "fi\n" +
               "\n" +
               "if [[ \$PROP_FILE =~ \$cil_regex ]]; then\n" +
               "    PROP_FILE=`echo \"\$PROP_FILE\" | sed -r \"s/\$cil_regex/\\1\$CILVERSION/\"`\n"+
               "else\n" +
               "    PROP_FILE=`echo -e \"\$PROP_FILE\"\"\\nCIL_VERSION=\${CILVERSION}\"`\n" +
               "fi\n" +
               "\n" +
               "echo \"\$PROP_FILE\" > " + PATH_TO_JOB_CONFIG + tpgName +
                                       "_washingmachine_\${BRANCH}params.properties\n" +
               "echo \"\"\n" +
               "echo \"Washingmachine: " + tpgName + "_washingmachine_\${BRANCH} | MSV Version: " +
                                       "\${MSVVERSION} | CIL Version: \${CILVERSION}\"\n" +
               "echo \"\"\n"
    }
}
