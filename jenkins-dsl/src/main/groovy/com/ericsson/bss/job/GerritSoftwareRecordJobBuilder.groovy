package com.ericsson.bss.job

import com.ericsson.bss.AbstractGerritJobBuilder

import javaposse.jobdsl.dsl.Job

public class GerritSoftwareRecordJobBuilder extends AbstractGerritJobBuilder {

    private String projectName

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addMavenDependencyTestConfig()
        return job
    }

    public void addMavenDependencyTestConfig() {
        job.with {

            String jobDescription = "" +
                    "<h2>This job creates a Software Record</h2>"

            description(DSL_DESCRIPTION + jobDescription)
            concurrentBuild()
            parameters {
                stringParam('PRODUCTNAME', '', 'Name of the product')
                stringParam('PRODUCTREPOSITORY', '', 'Repository name')
                stringParam('ARTIFACTADDRESS', '', 'Address to ARM directory')
                stringParam('ARTIFACTID', '', 'Artifact id')
                stringParam('GROUPID', '', 'Group id')
                stringParam('VERSION', '', 'Version of Artifact')
                stringParam('REMOTETARGETDIR', '', 'Remote target directory in ARM')
                booleanParam('INCLUDESOURCES', false, 'Include sources in package')
            }

            steps {
                shell(softwareRecordCommand())
            }
            injectEnv(getInjectVariables())
            addTimeoutConfig()
        }
    }

    private String softwareRecordCommand() {
        return "" +
                "INCLUDESOURCESSTR=\"\"\n" +
                "if [[ \"\${INCLUDESOURCES}\" = \"true\" ]]; then \n" +
                "   INCLUDESOURCESSTR='--includesources'\n" +
                "fi\n" +
                getShellCommentDescription("Create Software Record") +
                "if [ \"\$VERSION\" != \"\" ]; then\n" +
                "sleep 60\n" +
                "cd /proj/env/eta/charging/charging/\n" +
                "./create_software_record.py --gerritaddress " + gerritServer + " --version \${VERSION} --product \${PRODUCTNAME} \\\n" +
                    "--productrepository \${PRODUCTREPOSITORY} --artifactaddress \${ARTIFACTADDRESS} --tempdir \${WS_TMP} \\\n" +
                    "--uploadtoarm --gituser " + gerritUser + " --mvnsettingsfile \${MAVEN_SETTINGS} \\\n" +
                    "--mvnrepository \${MAVEN_REPOSITORY} --mvntempdir \${WS_TMP} \\\n" +
                    " --mvnhome \${M2_HOME} --githome \${GIT_HOME} --javahome \${JAVA_HOME} \\\n" +
                    " --artifactid \${ARTIFACTID} --groupid \${GROUPID} --remotetargetdir \${REMOTETARGETDIR} \${INCLUDESOURCESSTR}\n" +
                "fi"
    }
}
