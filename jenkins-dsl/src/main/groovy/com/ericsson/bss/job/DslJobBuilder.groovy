package com.ericsson.bss.job

import com.ericsson.bss.AbstractJobBuilder
import javaposse.jobdsl.dsl.Job

class DslJobBuilder extends AbstractJobBuilder {

    protected List repositories

    protected String gitRepositoryName

    protected String jenkinsURL

    protected String gerritName

    protected Map environemntVariableMap = [:]

    private static final String JOB_DESCRIPTION = "<h2>Job that generate other Jenkins jobs</h2>\n" +
            "<p>This job generates other jobs by using the Jenkins DSL plugin.<br/>\n" +
            "In this way we can have the job configuration in SCM and also able to easily generate the job configuration locally for testing.<br/></p>\n" +
            "<p>Please see <a href=\"https://gerrit.epk.ericsson.se/plugins/gitiles/tools/eta/jenkins-dsl/+/refs/heads/master/README.md\">README.md</a>" +
            " for more info regarding the DSL.</p>"

    public Job build() {
        timeoutForJob = 40
        initProject(dslFactory.freeStyleJob(jobName))
        addGitRepository(gitRepositoryName)
        addDslConfig()
        return job
    }

    protected void addDslConfig() {
        job.with {
            description(DSL_DESCRIPTION + JOB_DESCRIPTION)

            customWorkspace(CUSTOM_WORKSPACE_MESOS)

            addPermissionConfig()

            environemntVariableMap.put('project_name', gerritName)
            wrappers {
                environmentVariables { envs(environemntVariableMap) }
            }
            addTimeoutConfig()

            steps {
                gradle {
                    gradleName('Gradle 2.4')
                    useWrapper(false)
                    tasks('clean test')
                    fromRootBuildScriptDir(true)
                    useWorkspaceAsHome(true)
                }

                dsl{
                    removeAction('IGNORE')
                    external('jobs/bssJobs.groovy')
                    ignoreExisting(false)
                    additionalClasspath('src/main/groovy')
                }
            }

            publishers { wsCleanup() }
        }
    }

    protected void addPermissionConfig(){
        job.with {
            environemntVariableMap.put('PERMISSION_FILE', getPermissionFile())

            publishers {
                flexiblePublish {
                    conditionalAction {
                        condition { alwaysRun() }
                        steps { shell(dslFactory.readFileFromWorkspace('scripts/set_job_permissions.sh')) }
                    }
                }

                mailer('', false, true)
            }
        }
    }

    private String getPermissionFile() {
        if (jenkinsURL.contains('internal')) {
            return 'jenkins-ka_perms.json'
        }
        else {
            return 'jenkins-ef_perms.json'
        }
    }
}
