package com.ericsson

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

class SyncJobBuilder extends AbstractCassandraJob {

    String syncScriptFile

    Job build(DslFactory dslFactory) {

        dslFactory.job(name) {
            it.description this.description + CASSANDRA_TROUBLESHOOT
            label(RESTRICT_LABEL_MESOS)
            customWorkspace(this.workspace)
            logRotator(10, 10, 5, 5)

            wrappers {
                environmentVariables { envs('PATH':'/opt/local/dev_tools/git/2.2.0/bin:\${PATH}') }
                timestamps()
            }

            triggers { cron 'H */2 * * *' }
            steps { shell this.syncScriptFile }

            publishers {
                wsCleanup()
                mailer('cassandra-automation@mailman.lmera.ericsson.se', true, false)
            }
        }
    }
}
