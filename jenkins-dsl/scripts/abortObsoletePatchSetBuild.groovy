import groovy.json.JsonSlurper
import hudson.model.Result

gerritServer = build.getEnvironment(listener).get("GERRIT_HOST")
changeId = build.getEnvironment(listener).get("GERRIT_CHANGE_NUMBER")
buildRevision = build.getEnvironment(listener).get("GERRIT_PATCHSET_REVISION")

sout = new StringBuffer()
serr = new StringBuffer()

for (int i = 0; i < 5; i++) {
    try {
        checkPatch()
        break
    } catch (Exception e) {
        if (i < 4) {
            sleep(1000)
            continue
        } else {
            build.setDescription('Build failed due to gerrit script error')
            println('Gerrit script error: ' + e.getMessage())
            build.getExecutor().interrupt(Result.FAILURE)
        }
    }
}

def checkPatch() {
    sout.setLength(0)
    serr.setLength(0)

    Process process = "ssh -p 29418 ${gerritServer} gerrit query --format=JSON --current-patch-set change:${changeId}".execute()
    process.consumeProcessOutput(sout, serr)
    process.waitFor()

    def exitCode = process.exitValue()
    if (exitCode != 0) {
        def msg = "Error running gerrit ssh query: " + serr.toString()
        println msg
        throw new Exception(msg)
    }

    def response = sout.toString().split("\n")[0]
    def json = parseJSON(response)
    if (json?.type == "error") {
        def msg = "Error running gerrit ssh query: " + json.message
        println msg
        throw new Exception(msg)
    }

    def currentRevision = json?.currentPatchSet?.revision
    println "Current patch set revision :${currentRevision}"
    println "Build patch set revision :${buildRevision}"

    if (buildRevision != currentRevision)
        abortBuild('Build aborted due to obsolete patch set')

    if (json?.status?.toLowerCase() == 'abandoned')
        abortBuild('Build aborted due to abandoned patch')
}

def parseJSON(String text) {
    def slurper = new JsonSlurper()
    def result = slurper.parseText(text)
    return result
}

def abortBuild(String description) {
    println description
    build.setDescription(description)
    build.getExecutor().interrupt(Result.ABORTED)
}
