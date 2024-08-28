package com.ericsson.bss.job.cpm

import com.ericsson.bss.job.washingmachine.WashingMachineReleaseBranchJobBuilder

class CpmWashingMachineReleaseBranchJobBuilder extends WashingMachineReleaseBranchJobBuilder {

    @Override
    protected Map injectVariables() {
        Map env = super.injectVariables()
        env.put("JAVA_HOME", "/opt/local/dev_tools/java/x64/latest-1.7")
        env["M2_HOME"] = "/opt/local/dev_tools/maven/apache-maven-3.2.3"
        env["JAVA_TOOL_OPTIONS"] = "-Xss1M -Xms64m -Xmx8G -XX:MaxPermSize=512m -XX:SelfDestructTimer=\${JOB_TIMEOUT} -Djava.io.tmpdir=\${WS_TMP}"
        env["MAVEN_OPTS"] = "-server -Xss1M -Xms64m -Xmx8G -XX:MaxPermSize=512m -XX:SelfDestructTimer=\${JOB_TIMEOUT} -Djava.io.tmpdir=\${WS_TMP}"

        return env
    }

    @Override
    protected String getAdditionalTapasShell() {
        return '\nrm -rf ${WORKSPACE}/.repository\n'
    }

    @Override
    protected void setExtraShell() {
        job.with {
            steps {
                shell(dslFactory.readFileFromWorkspace('scripts/washingmachine/cpm_washingmachine_pre_tapas.sh'))
            }
        }
    }
}