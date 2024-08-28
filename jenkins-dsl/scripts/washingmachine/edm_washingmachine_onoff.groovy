import jenkins.model.*

def instance = Jenkins.getInstance()

String reason = build.buildVariableResolver.resolve("REASON")
String user = build.getEnvironment(listener).get("BUILD_USER_ID")
String jobname = "edm_washingmachine_keepalive"
def job = instance.getItem(jobname)

hostSet = build.buildVariableResolver.resolve("ENABLED_HOSTS")

if (hostSet == "none") {
    println "Disabling " + jobname
    desc = "Job disabled by " + user + " with reason: '" + reason + "'"
    println "With description: " + desc
    job.disable()
    job.setDescription(desc)
} else {
    def hostSetDescription = "all hosts"
    if (hostSet != "alternate") {
        hostSetDescription = "host ${hostSet}"
    }
    println "Enabling " + jobname
    desc = "Job enabled (for ${hostSetDescription}) by " + user + " with reason: '" + reason + "'"
    println "With description: " + desc
    job.enable()
    job.setDescription(desc)
}

prepareParams(hostSet == "none" ? false : true)

private void prepareParams(boolean jobEnabled) {
    Properties props = new Properties()
    File propsFile = new File('<PARAMS_FILES_DIR>')
    props.load(propsFile.newDataInputStream())

    props.setProperty('HOST_SET', hostSet)
    props.setProperty('JOB_ENABLED', Boolean.toString(jobEnabled))
    props.setProperty('STAGINGVERSION', 'LATEST')
    props.store(propsFile.newWriter(), null)

    println 'Content of the params file:'
    println propsFile.text
}
