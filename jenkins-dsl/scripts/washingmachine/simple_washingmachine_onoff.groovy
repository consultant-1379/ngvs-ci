import jenkins.model.*

def instance = Jenkins.getInstance()
String action = build.buildVariableResolver.resolve("ACTION")
String reason = build.buildVariableResolver.resolve("REASON")
String user = build.getEnvironment(listener).get("BUILD_USER_ID")
String jobName = "<PROJECT_TO_BUILD_NAME>_keepalive"
def job = instance.getItem(jobName)

String buildDesc = action + " by " + user
build.setDescription(buildDesc)
println "Setting build description to: " + buildDesc

if (action == "Enable") {
    println "Enabling " + jobName
    desc = "Job enabled by " + user + " with reason: '" + reason + "'"
    println "With description: " + desc
    job.enable()
    job.setDescription(desc)
} else {
    println "Disabling " + jobName
    desc = "Job disabled by " + user + " with reason: '" + reason + "'"
    println "With description: " + desc
    job.disable()
    job.setDescription(desc)
}

prepareParams(action == "Enable" ? true : false)

private void prepareParams(boolean jobEnabled) {
    Properties props = new Properties()
    File propsFile = new File('<PARAMS_FILES_DIR>')
    props.load(propsFile.newDataInputStream())

    props.setProperty('JOB_ENABLED', Boolean.toString(jobEnabled))
    props.store(propsFile.newWriter(), null)

    println 'Content of the params file:'
    println propsFile.text
}
