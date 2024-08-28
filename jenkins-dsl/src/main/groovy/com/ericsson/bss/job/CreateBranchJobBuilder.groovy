package com.ericsson.bss.job

import com.ericsson.bss.AbstractJobBuilder
import javaposse.jobdsl.dsl.Job
import com.ericsson.bss.util.GitUtil

public class CreateBranchJobBuilder extends AbstractJobBuilder {

    private static final String JOB_DESCRIPTION = "<h2>Create a new release branch.</h2>" +
    DETAILED_JOB_CONFIGURATION_DESCRIPTION +
    "<p>This job automate the process described on " +
    "<a href='https://eta.epk.ericsson.se/maven-sites/latest/" +
    "com.ericsson.bss.rm.charging/parent/site/component_rerelease.html'>rerelease process</a> page.</p>" +
    "<p>To be able to trigger this job you need to have the release " +
    "<a href='https://eta.epk.ericsson.se/maven-sites/latest/com.ericsson.bss.rm.charging/parent/site/jenkins.html#jenkins_job_permissions'>" +
    "permissions</a>.</p>" +
    "<dl>\n" +
    "<dt>Prerequisite</dt>\n" +
    "<dd>Can only create release branch from X.Y.0.</dd>\n" +
    "</dl>"

    protected String gerritName

    private String createBranchScriptFile = 'scripts/create_branch.sh'

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addCreateBranchConfig()
        return job
    }

    public void addCreateBranchConfig() {

        String gitURL = GitUtil.getGitUrl(gerritServer, gerritName)

        job.with {

            description(DSL_DESCRIPTION + JOB_DESCRIPTION)

            customWorkspace(workspacePath)
            scm {
                git {
                    remote { url(gitURL) }
                    branch('origin/master')
                }
            }
            configure { project ->
                project / scm / extensions << 'hudson.plugins.git.extensions.impl.UserIdentity' {
                    name(gitName)
                    email(gitEmail)
                }
                project / scm / extensions << 'hudson.plugins.git.extensions.impl.CloneOption' {
                    shallow(false)
                    reference(GitUtil.getCloneReference())
                    timeout(timeoutForJob)
                }
            }

            parameters {
                activeChoiceParam('GIT_TAG') {
                    description("Tag to create release branch from.")
                    choiceType('SINGLE_SELECT')
                    groovyScript {
                        script('''
                            def version_list = []

                            String git_repo = "gitURL"

                            String p4 = "/opt/local/dev_tools/git/latest/bin/git ls-remote --tags " + git_repo + " | awk -F ' ' '{print \\\$2}' | grep -P '(?<!\\\\^{})\\\$' | sort -rV"

                            def output = ['bash', '-c', p4].execute().in.text

                            output.tokenize('\\n').each {
                              if ( it[-1]  ==  '0' ) {
                                version_list.add(it[10..-1])
                              }
                            }

                            return version_list
                        '''.replaceFirst('gitURL', gitURL).stripIndent())
                        fallbackScript('return ["Error evaluating Groovy script.", ""]')
                    }
                }
            }

            injectEnv(getInjectVariables())

            steps {
                /*
                 * TODO:discussions about how to re-trigger dsl job,
                 * just give a not to trigger dsl, or can a mail be sent to ETA.
                 */
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
                shell(removeOldArtifacts())
                shell(gitConfig("\${WORKSPACE}"))
                if (gerritServer.equalsIgnoreCase(GitUtil.GERRIT_CENTRAL_SERVER)) {
                    shell(this.gitPushToCentral())
                }
                shell(createBranchShell())
            }

            addTimeoutConfig()

            publishers {
                buildDescription("^Creating branch \\(should removed last digit\\)(.*)",
                        "", "^Creating branch \\(should removed last digit\\)(.*)", "", false)

                downstream(projectName + '_dsl', 'SUCCESS')

                wsCleanup()
            }
        }
    }

    protected String gitPushToCentral() {
        String gitCentralURL = GitUtil.getGitUrl(gerritServer, gerritName).replace(GitUtil.GERRIT_CENRAL_SERVER_MIRROR, GitUtil.GERRIT_CENTRAL_SERVER)

        return getShellCommentDescription('Push to central') +
                'git remote set-url origin --push ' + gitCentralURL
    }

    protected String createBranchShell() {
        if (createBranchScriptFile.equals("")) {
            return ""
        }
        else {
            String createBranchScript = dslFactory.readFileFromWorkspace(createBranchScriptFile)

            if (profilesToBeUsed != null && !profilesToBeUsed.equals("")) {
                createBranchScript = createBranchScript.replace('release:branch', 'release:branch -P' + profilesToBeUsed)
            }

            return createBranchScript
        }
    }
}
