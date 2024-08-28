package com.ericsson.bss.job

import com.ericsson.bss.AbstractJobBuilder
import javaposse.jobdsl.dsl.Job

public class BackwardCompatibilityTestJobBuilder extends AbstractJobBuilder {
    protected String projectName
    protected String gerritName
    private String mailRecipients

    private String replaceIntegrationTestScriptFile = 'scripts/replaceintegrationtest.sh'

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addBackwardCompatibilityTestConfig()
        return job
    }

    private void addBackwardCompatibilityTestConfig() {
        job.with {
            disabled(true)
            String jobDescription = "" +
            "<h2>Runs latest released Integration test for repository.</h2>" +
            "<p>This job will test backward compatibility by using the latest version of Integration test " +
            "on the SNAPSHOT version.</p>"

            description(DSL_DESCRIPTION + jobDescription)
            triggers {
                upstream(projectName + '_deploy', 'SUCCESS')
            }

            deleteWorkspaceBeforeBuildStarts()

            def env_list = getInjectVariables()
            env_list.put("GERRIT_USER", gerritUser)
            env_list.put("GERRIT_SERVER", gerritServer)
            env_list.put("GERRIT_NAME", gerritName)
            env_list.put("PROJECT_NAME", projectName)

            //Fails with file not found if "-Djava.io.tmpdir=\${WS_TMP}" is set
            env_list.put("JAVA_TOOL_OPTIONS", env_list.get("JAVA_TOOL_OPTIONS").replace("-Djava.io.tmpdir=\${WS_TMP}", ""))
            env_list.put("MAVEN_OPTS", env_list.get("MAVEN_OPTS").replace("-Djava.io.tmpdir=\${WS_TMP}", ""))

            injectEnv(env_list)

            steps {
                shell(replaceIntegrationTestShell())
                maven {
                    goals('clean')
                    goals('package')
                    //this is required since a method cannot be executed directly in mavenInstallation() step
                    String mavenInstallationName = getMavenInstallationName()
                    mavenInstallation(mavenInstallationName)
                    rootPOM(projectName + "/pom.xml")
                }
            }

            addExtendableEmail()
        }
    }

    private String replaceIntegrationTestShell() {
        String replaceIntegrationTestScript = dslFactory.readFileFromWorkspace(replaceIntegrationTestScriptFile)
        return replaceIntegrationTestScript
    }

    @Override
    protected void addExtendableEmail() {
        String recipients = '\$DEFAULT_RECIPIENTS'

        if (mailRecipients != null) {
            recipients += ', ' + mailRecipients
        }

        super.addExtendableEmail(recipients)
    }
}