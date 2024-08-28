package com.ericsson.bss.job

import com.ericsson.bss.AbstractTapasJobBuilder
import javaposse.jobdsl.dsl.Job

class CilTargethostRollbackJobBuilder extends AbstractTapasJobBuilder {

    public static final ArrayList<String> SNAPSHOT_NAMES = []

    CilTargethostRollbackJobBuilder() {
        suite = 'revert_cil.xml'

        SNAPSHOT_NAMES.add('baseline')
        SNAPSHOT_NAMES.add('baseline_tpg')
        defaultTapasJobPath = 'CIL/CIL targethost rollback'.replace(' ', '%20')
    }

    public static final String JOB_DESCRIPTION =
            "<p>This job performs a rollback of the CIL targethost, which can be \n" +
            "provided manually or selected from the TPG's cluster.<br>\n" +
            "Normally the CIL rollback is being done automatically during the " +
            "installation.\nThis job allows to perform the rollback manually, " +
            "if there is a need to perform\nan additional cleanup without a complete \n" +
            "reinstallation. <br>Logs from the performed rollback can be found in \n" +
            "<a href=\"https://tapas.epk.ericsson.se/#/suites/CIL/CIL%20targethost%20rollback\">\n[TAPAS] CIL targethost rollback</a>."

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        setProjectDescription(JOB_DESCRIPTION)
        return job
    }

    @Override
    protected void setInputParameters() {
        addSelectClusterParameter('Choose the cluster to rollback.')
        addClusterReferenceParameter('CIL', 'The CIL targethost. Before running this job it should be installed and it has to have the selected snapshot.')
        addListChoiceParam('SNAPSHOT_NAME', SNAPSHOT_NAMES, 'The rollback will be performed to the snapshot selected here.')
        }

    @Override
    protected String getTapasParameters() {
        String params = super.getTapasParameters()
        params += '--define=__TARGETHOST__="${CIL}" \\\n'
        params += '--define=__SNAPSHOT_NAME__="${SNAPSHOT_NAME}" \\\n'
        params += '--define=__VMAPI_PROFILE_PREFIX__="' + projectName + '." \\\n'
        return params
    }

    @Override
    protected void setTapasShell() {
        job.with {
            steps {
                shell(getTapasShell())
            }
        }
    }

    @Override
    protected String getTapasConfigSettings() {
        return 'BASE_CONFIG_FILE="/proj/eta-automation/tapas/config/cil/suites/installnode/' +
                suite + '"\n' + 'CONFIG_FILE="/proj/eta-automation/tapas/sessions/cil/' +
                suite.replace('.xml', '-\${CIL}.xml"\n')
    }
}
