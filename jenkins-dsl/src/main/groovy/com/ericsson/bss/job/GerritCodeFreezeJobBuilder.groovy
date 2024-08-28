package com.ericsson.bss.job

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.util.GitUtil
import javaposse.jobdsl.dsl.Job

class GerritCodeFreezeJobBuilder extends AbstractJobBuilder {

    protected List repositories
    protected String approvers

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addGitRepository(gerritName)
        addCodeFreezeConfig()
        return job
    }

    protected void addCodeFreezeConfig() {
        String gitURL = GitUtil.getGitUrl(gerritServer, gerritName)

        job.with {

            String jobDescription = "Performs code (un)freeze on branches in specified repositories<br/>\n" +
                    "For issues release to this job see " +
                    "<a href='https://eta.epk.ericsson.se/wiki/index.php5/Gerrit_Code_Freeze_Script_troubleshoot'>Gerrit Code Freeze Script troubleshoot</a>."

            description(DSL_DESCRIPTION + jobDescription)
            scm {
                git {
                    remote { url(gitURL) }
                    branch('origin/master')
                    extensions {
                        cloneOptions {
                            shallow(false)
                            reference(GitUtil.getCloneReference())
                            timeout(timeoutForJob)
                        }
                    }
                }
            }
            configure { project ->
                project / scm / extensions << 'hudson.plugins.git.extensions.impl.UserIdentity' {
                    name(gitName)
                    email(gitEmail)
                }
                /*
                The parameter definitions below needs to be done via the "configure" instead of DSL directly,
                due to that "Validating String Parameter" does not support DSL. This means that if the order
                of the parameters is to be in a correct logical order, all parameters needs to be defined via
                "configure". If not, the order of the parameters will not be the same as they are written in
                this file.
                 */
                project / 'properties' / 'hudson.model.ParametersDefinitionProperty' / parameterDefinitions << 'org.biouno.unochoice.ChoiceParameter' {
                    name('REPOSITORIES_CODE_FREEZE')
                    description("Choose one or more repositories to do code freeze on.")
                    randomName('choice-parameter-1')
                    visibleItemCount(1)
                    script(class: 'org.biouno.unochoice.model.GroovyScript') {
                        script(getRepositoriesAsReturnValue())
                        fallbackScript('return ["Error evaluating Groovy script.", ""]')
                    }
                    filterable(true)
                    choiceType('PT_MULTI_SELECT')
                }
                project / 'properties' / 'hudson.model.ParametersDefinitionProperty' /
                        parameterDefinitions << 'hudson.plugins.validating__string__parameter.ValidatingStringParameterDefinition' {
                    name('BRANCHES_TO_FREEZE')
                    description('Supply the branches to be frozen for the chosen repositories above. Separate each branch with space.')
                    regex(/^[a-zA-Z0-9_ \/.]*\u0024/)
                    failedValidationMessage("Only space separated alphanumeric strings, that can contain '_' and/or '/', are accepted.")
                }
                project / 'properties' / 'hudson.model.ParametersDefinitionProperty' /
                        parameterDefinitions << 'hudson.plugins.validating__string__parameter.ValidatingStringParameterDefinition' {
                    name('BRANCHES_TO_UNFREEZE')
                    description('Supply the branches to be unfrozen for the chosen repositories above. Separate each branch with space.')
                    regex(/^[a-zA-Z0-9_ \/.]*\u0024/)
                    failedValidationMessage("Only space separated alphanumeric strings, that can contain '_' and/or '/', are accepted.")
                }
                project / 'properties' / 'hudson.model.ParametersDefinitionProperty' /
                        parameterDefinitions << 'hudson.plugins.validating__string__parameter.ValidatingStringParameterDefinition' {
                    name('APPROVERS')
                    defaultValue(approvers)
                    description("The numeric Gerrit-id's for each user that should be able to do submit during code freeze. Separate each id with space.")
                    regex(/^[0-9 ]*\u0024/)
                    failedValidationMessage('Only space separated numeric strings are accepted.')
                }
            }
            steps {
                shell(gerritCodeFreezeCommand())
            }
        }
    }

    private String getRepositoriesAsReturnValue() {
        return "return['" + repositories.join("','") + "']"
    }

    private String gerritCodeFreezeCommand() {
        String command =
                "set -x\n" +
                "set -e\n\n" +
                "/proj/env/bin/python -u bin/gerrit_code_freeze.py \\\n" +
                "--repositories `echo \${REPOSITORIES_CODE_FREEZE} | tr ',' ' '` \\\n" +
                "--branch-freeze \${BRANCHES_TO_FREEZE} \\\n" +
                "--branch-un-freeze \${BRANCHES_TO_UNFREEZE} \\\n" +
                "--approvers \${APPROVERS}"
        return command
    }
}
