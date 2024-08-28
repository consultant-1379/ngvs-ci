import jenkins.model.*

def instance = Jenkins.getInstance()

String reason = build.buildVariableResolver.resolve("REASON")
String user = build.getEnvironment(listener).get("BUILD_USER_ID")
String jobName = "charging_washingmachine_rpm_keepalive"
def job = instance.getItem(jobName)

hostSet = build.buildVariableResolver.resolve("ENABLED_HOSTS")
coreRpmVersion = build.buildVariableResolver.resolve("CORERPMVERSION")
accessRpmVersion = build.buildVariableResolver.resolve("ACCESSRPMVERSION")
dlbRpmVersion = build.buildVariableResolver.resolve("DLBRPMVERSION")
jiveVersion = build.buildVariableResolver.resolve("JIVEVERSION")
serviceRpmVersion = build.buildVariableResolver.resolve("SERVICERPMVERSION")

if (hostSet == "none") {
    println "Disabling " + jobName
    desc = "Job disabled by " + user + " with reason: '" + reason + "'"
    println "With description: " + desc
    job.disable()
    job.setDescription(desc)
} else {
    def hostSetDescription = "all hosts"
    if (hostSet != "alternate") {
        hostSetDescription = "host ${hostSet}"
    }
    println "Enabling " + jobName
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
    props.setProperty('ACCESSRPMVERSION', accessRpmVersion)
    props.setProperty('CORERPMVERSION', coreRpmVersion)
    props.setProperty('DLBRPMVERSION', dlbRpmVersion)
    props.setProperty('JIVEVERSION', jiveVersion)
    props.setProperty('SERVICERPMVERSION', serviceRpmVersion)
    props.setProperty('ACCESSSTAGINGVERSION', 'LATEST')
    props.setProperty('CORESTAGINGVERSION', 'LATEST')
    props.setProperty('JOB_ENABLED', Boolean.toString(jobEnabled))
    props.store(propsFile.newWriter(), null)

    println 'Content of the params file:'
    println propsFile.text
}
