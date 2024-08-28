package com.ericsson.bss.job

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.util.GitUtil
import javaposse.jobdsl.dsl.Job

public class NpmCreatePatchBranchJobBuilder extends AbstractJobBuilder {

    private String createBranchScriptFile = 'scripts/npm_create_patch_branch.sh'
    private String gitTagScriptFile = 'scripts/npm_get_patch_branch_candidates.groovy'
    private String gitURL = ""

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addCreatePatchBranchConfig()
        return job
    }

    @Override
    protected getInjectVariables() {
        Map envList = super.getInjectVariables()

        envList.remove("PATH")
        envList.put("NPM_MODULES", "\${WORKSPACE}/node_modules/.bin")
        envList.put("NODE_PATH", "/opt/local/dev_tools/nodejs/node-v5.9.1-linux-x64/bin/")
        envList.put("NPM_CONFIG_USERCONFIG", "/proj/eta-automation/config/kascmadm/.npmrc")
        envList.put("PATH", "\${PYTHONPATH}/bin:\${GIT_HOME}/bin:\${M2}:\${NODE_PATH}:\${NPM_MODULES}:\${PATH}")
        envList.put("REPOSITORY_URL", gitURL)

        return envList
    }

    public void addCreatePatchBranchConfig() {
        gitURL = GitUtil.getGitUrl(gerritServer, gerritName)

        job.with {

            String jobDescription = "<h2>Create a new patch branch.</h2>" +
                    "<p>This job is used to create a new patch branch for a selected\n" +
                    "release. For the detailed description of the deployment process see \n" +
                    "<a href=\"https://eta.epk.ericsson.se/wiki/index.php5/NPM_release_process\" " +
                    "target=\"_blank\">NPM release process</a> wiki page.\n"

            description(DSL_DESCRIPTION + jobDescription)
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
                    description("Tag to create release branch from. If the list is empty, " +
                                "patch branches were already created for all existing releases.")
                    choiceType('SINGLE_SELECT')
                    groovyScript {
                        script( getGitTatScript() )
                        fallbackScript('return ["Error evaluating Groovy script.", ""]')
                    }
                }
            }

            injectEnv(getInjectVariables())

            steps {
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
                shell(removeOldArtifacts())
                shell(gitConfig("\${WORKSPACE}"))
                shell(installNpmSnapshotModule())
                shell(installNpmVersionnModule())
                shell(createPatchBranchShellScript())
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

    private String createPatchBranchShellScript() {
        return  dslFactory.readFileFromWorkspace(createBranchScriptFile)
    }

    protected String installNpmSnapshotModule() {
        return getShellCommentDescription("Install npm-snapshot module") +
                "npm install npm-snapshot"
    }

    protected String installNpmVersionnModule() {
        return getShellCommentDescription("Install versionn module") +
                "npm install versionn"
    }

    protected String getGitTatScript() {
        return dslFactory.readFileFromWorkspace(gitTagScriptFile)
                         .replace("%GIT_URL%", gitURL).stripIndent()
    }
}
