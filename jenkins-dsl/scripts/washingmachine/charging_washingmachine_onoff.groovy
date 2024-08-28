import jenkins.model.*

def instance = Jenkins.getInstance()

String reason = build.buildVariableResolver.resolve("REASON")
String user = build.getEnvironment(listener).get("BUILD_USER_ID")
String jobName = "charging_washingmachine_keepalive"
def job = instance.getItem(jobName)

hostSet = build.buildVariableResolver.resolve("ENABLED_HOSTS")
coreOvfVersion = build.buildVariableResolver.resolve("COREOVFVERSION")
accessOvfVersion = build.buildVariableResolver.resolve("ACCESSOVFVERSION")
dlbOvfVersion = build.buildVariableResolver.resolve("DLBOVFVERSION")
jiveVersion = build.buildVariableResolver.resolve("JIVEVERSION")

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
    props.setProperty('ACCESSOVFVERSION', accessOvfVersion)
    props.setProperty('COREOVFVERSION', coreOvfVersion)
    props.setProperty('DLBOVFVERSION', dlbOvfVersion)
    props.setProperty('JIVEVERSION', jiveVersion)
    props.setProperty('ACCESSSTAGINGVERSION', 'LATEST')
    props.setProperty('CORESTAGINGVERSION', 'LATEST')
    props.setProperty('DLBSTAGINGVERSION', 'LATEST')
    props.setProperty('JOB_ENABLED', Boolean.toString(jobEnabled))
    props.store(propsFile.newWriter(), null)

    println 'Content of the params file:'
    println propsFile.text
}
