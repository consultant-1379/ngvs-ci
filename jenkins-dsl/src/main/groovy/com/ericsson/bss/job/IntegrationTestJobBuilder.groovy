package com.ericsson.bss.job

import com.ericsson.bss.AbstractJobBuilder
import javaposse.jobdsl.dsl.Job

public class IntegrationTestJobBuilder extends AbstractJobBuilder {
    private String gerritName
    private String mailRecipients
    private String targetGerrit
    private String projectName

    private String integrationTestScriptFile = 'scripts/integrationtest.sh'
    private String integrationTestReleasedScriptFile = 'scripts/integrationtest_released.sh'

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))

        addIntegrationTestConfig()
        return job
    }

    private void addIntegrationTestConfig() {
        job.with {
            String jobDescription = "" +
                    "<h2>" + projectName.capitalize() + " integration test for TPG</h2>" +
                    "<p>Executes integration test for TPG with latest version of " +
                    projectName + "</p>"

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
            env_list.put("TARGET_GERRIT_NAME", targetGerrit)
            env_list.put("GIT_CLONE_CACHE", "/workarea/bss-f_gen/kascmadm/.gitclonecache")

            //Fails with file not found if "-Djava.io.tmpdir=\${WS_TMP}" is set
            env_list.put("JAVA_TOOL_OPTIONS", env_list.get("JAVA_TOOL_OPTIONS").replace("-Djava.io.tmpdir=\${WS_TMP}", ""))
            env_list.put("MAVEN_OPTS", env_list.get("MAVEN_OPTS").replace("-Djava.io.tmpdir=\${WS_TMP}", ""))

            injectEnv(env_list)

            steps {
                shell(cloneAndBuildShell())
                shell(integrationTestShell())
            }
            publishers {
                archiveJunit(targetGerrit.split("/")[1] + '/**/target/surefire-reports/*.xml')

                wsCleanup()
            }
            addExtendableEmail()
        }
    }

    private String integrationTestShell() {
        String integrationTestScript = ""

        if (jobName.contains("_cha")) {
            integrationTestScriptFile = 'scripts/integrationtest_cha.sh'
            integrationTestReleasedScriptFile = 'scripts/integrationtest_released_cha.sh'
        }

        if (jobName.contains("released")) {
            integrationTestScript =
                dslFactory.readFileFromWorkspace(integrationTestReleasedScriptFile)
        } else {
            integrationTestScript =
                dslFactory.readFileFromWorkspace(integrationTestScriptFile)
        }
        return integrationTestScript
    }

    @Override
    protected void addExtendableEmail() {
        String recipients = '\$DEFAULT_RECIPIENTS'

        if (mailRecipients != null) {
            recipients += ', ' + mailRecipients
        }

        super.addExtendableEmail(recipients)
    }

    private String cloneAndBuildShell() {
        return "# Clone and build " + projectName +
        "\ngit clone ssh://" + gerritUser +
        "@" + gerritServer +":29418/" + gerritName +
        " " + projectName + " --reference=\${GIT_CLONE_CACHE}" +
        "\ncd " + projectName +
        "\nmvn clean install -DskipTests -settings \${MAVEN_SETTINGS} " +
        "-Dorg.ops4j.pax.url.mvn.settings=\${MAVEN_SETTINGS} " +
        "-Dmaven.repo.local=\${MAVEN_REPOSITORY} " +
        "-Dorg.ops4j.pax.url.mvn.localRepository=\${MAVEN_REPOSITORY}"
    }
}
