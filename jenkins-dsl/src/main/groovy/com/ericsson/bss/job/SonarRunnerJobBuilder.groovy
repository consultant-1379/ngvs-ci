package com.ericsson.bss.job

import com.ericsson.bss.util.GitUtil

class SonarRunnerJobBuilder extends SonarJobBuilder {

    protected static String sonarRunnerProperties = "sonar.host.url=https://sonar.epk.ericsson.se/\n" +
            "sonar.jdbc.url=jdbc:mysql://sonar.epk.ericsson.se:3306/sonar?" +
            "useUnicode=true&characterEncoding=utf8&" +
            "rewriteBatchedStatements=true&useConfigs=maxPerformance\n" +
            "sonar.jdbc.username=sonar\n" +
            "sonar.jdbc.password={aes}XGL6Xw5N2OF2QNfd8g1GBw==\n" +
            "sonar.secretKeyPath=/proj/eta-automation/config/kascmadm/sonar/sonar-secret.txt\n" +
            "sonar.sourceEncoding=UTF-8"

    @Override
    protected void initShellJobs() {
        shells.add(cleanUpWorkspaceMesos("\${WORKSPACE}"))
        shells.add(gitConfig("\${WORKSPACE}"))
        String coverageCommand = getCoverageCommand()
        if (coverageCommand != null) {
            shells.add(getCoverageCommand())
        }
    }

    protected String getCoverageCommand() {
        return null
    }

    @Override
    protected void addSonarConfig() {
        job.with {
            description(DSL_DESCRIPTION + JOB_DESCRIPTION)

            addGitRepository(gerritName)
            customWorkspace(CUSTOM_WORKSPACE_MESOS)
            addTimeoutConfig()
            triggers {
                if (GitUtil.isLocatedInGitolite(gerritName)) {
                    scm(SCM_POLLING_FREQUENT)
                } else {
                    scm(SCM_POLLING + '\n# Realtime pushed by the eta_gitscmpoll_trigger job')
                }
            }
            steps {
                for (shellStep in shells) {
                    shell(shellStep)
                }
            }

            configure { project ->
                project / builders / 'hudson.plugins.sonar.SonarRunnerBuilder' / 'properties' <<
                        sonarRunnerProperties
            }

            String mavenProjectFolder = ""
            if (mavenProjectLocation) {
                mavenProjectFolder = mavenProjectLocation[0..-9] + "/"
            }

            configure { project ->
                project / builders / 'hudson.plugins.sonar.SonarRunnerBuilder' / 'project' <<
                        mavenProjectFolder + "sonar-runner.properties"
            }

            injectEnv(getInjectVariables())
            addExtendableEmail()
        }
    }
}
