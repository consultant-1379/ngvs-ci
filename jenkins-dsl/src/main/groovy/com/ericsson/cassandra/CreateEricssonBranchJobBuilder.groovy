package com.ericsson.cassandra

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

import com.ericsson.AbstractCassandraJob

class CreateEricssonBranchJobBuilder extends AbstractCassandraJob {

    Job build(DslFactory dslFactory) {
        dslFactory.job(name) {

            it.description this.description + CASSANDRA_TROUBLESHOOT

            label(RESTRICT_LABEL_MESOS)

            customWorkspace(this.workspace)

            logRotator(10, 10, 5, 5)

            parameters {
                activeChoiceParam('GIT_TAG') {
                    description("Tag to create release branch from.")
                    choiceType('SINGLE_SELECT')
                    groovyScript {
                        script(getCassandraGitTags())
                        fallbackScript('return ["Error evaluating Groovy script.", ""]')
                    }
                }

                choiceParam('BRANCH_PREFIX', ['ericsson', 'cil', 'vs'], 'Will create prefix for branch. \n' +
                'Standard to use ericsson, but possible to create product specific branches.')
            }

            wrappers { timestamps() }

            scm {
                git this.gitUrl, this.branch
            }

            steps {
                shell(changeRemotesShell())
                shell dslFactory.readFileFromWorkspace('scripts/cassandra_create_ericsson_branch.sh')
            }
        }
    }

    private String changeRemotesShell() {
        return "" +
                "###########################\n" +
                "#  Change push to central #\n" +
                "###########################\n" +
                "git remote set-url origin ssh://gerritmirror.lmera.ericsson.se:29418/cassandra/cassandra \n" +
                "git remote set-url origin --push ssh://gerrit.ericsson.se:29418/cassandra/cassandra"
    }

    private String getCassandraGitTags(){
        return "" +
                "def version_list = []\n" +
                "String git_repo = \"" + this.gitUrl + "\"\n" +
                "String p4 = \"/opt/local/dev_tools/git/latest/bin/git ls-remote --tags \" + git_repo + \" | awk -F ' ' '{print \\\$2}' | grep -P '(?<!\\\\^{})\\\$' | sort -rV\"\n" +
                "def output = ['bash', '-c', p4].execute().in.text\n" +
                "output.tokenize('\\n').each {\n" +
                "  if ( it.contains('cassandra-') ) {\n" +
                "    version_list.add(it[10..-1])\n" +
                "  }\n" +
                "}\n" +
                "return version_list"
    }
}
