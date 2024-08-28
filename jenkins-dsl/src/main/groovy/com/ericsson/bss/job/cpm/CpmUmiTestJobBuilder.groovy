package com.ericsson.bss.job.cpm

import com.ericsson.bss.job.UmiTestJobBuilder

class CpmUmiTestJobBuilder extends UmiTestJobBuilder {

    @Override
    protected void setEnvVariables() {
        Map env_list = getInjectVariables()
        env_list.remove("HOME")
        env_list.put("JAVA_HOME", "/opt/local/dev_tools/java/x64/latest-1.7")
        env_list["M2_HOME"] = "/opt/local/dev_tools/maven/apache-maven-3.2.3"
        env_list["JAVA_TOOL_OPTIONS"] = "-Xss1M -Xms64m -Xmx8G -XX:MaxPermSize=512m -XX:SelfDestructTimer=\${JOB_TIMEOUT} -Djava.io.tmpdir=\${WS_TMP}"
        env_list["MAVEN_OPTS"] = "-server -Xss1M -Xms64m -Xmx8G -XX:MaxPermSize=512m -XX:SelfDestructTimer=\${JOB_TIMEOUT} -Djava.io.tmpdir=\${WS_TMP}"
        injectEnv(env_list)
    }
}
