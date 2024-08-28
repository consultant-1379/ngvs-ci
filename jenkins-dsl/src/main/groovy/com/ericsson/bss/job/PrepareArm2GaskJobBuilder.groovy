package com.ericsson.bss.job

import com.ericsson.bss.AbstractGerritJobBuilder

import javaposse.jobdsl.dsl.Job

public class PrepareArm2GaskJobBuilder extends AbstractGerritJobBuilder {

    private String projectName

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addMavenDependencyTestConfig()
        return job
    }

    public void addMavenDependencyTestConfig() {
        job.with {

            String jobDescription = "" +
                    "<h2>This job creates a Artifact and prepares it for Arm2Gask storage</h2>"

            description(DSL_DESCRIPTION + jobDescription)
            concurrentBuild()
            parameters {
                stringParam('PRODUCTREPOSITORY', '', 'Repository name, same as in gerrit')
                stringParam('VERSION', '', 'Version of Artifact')
            }

            steps {
                shell(prepareArm2GaskCommand())
            }
            injectEnv(getInjectVariables())
            addTimeoutConfig()
        }
    }

    private String prepareArm2GaskCommand() {
        return "" +
                getShellCommentDescription("Prepare Arm2Gask Record") +
                "if [[ \"\$VERSION\" != \"\" ]]; then\n" +
                "mkdir -p \$WS_TMP\n" +
                "cd /proj/env/eta/charging/charging/\n" +
                "./prepare_for_gask.py --gerritaddress " + gerritServer +
                " --tagversion \${VERSION} --repository \${PRODUCTREPOSITORY}" +
                " --mvnsettingsfile \${MAVEN_SETTINGS} --mvnrepository \${MAVEN_REPOSITORY} --mvntempdir \${WS_TMP}" +
                " --tempdir \${WS_TMP} --gituser " + gerritUser +
                "\n" +
                "fi"
    }
}
