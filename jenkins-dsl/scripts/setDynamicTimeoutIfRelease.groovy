import hudson.model.*
import hudson.util.*

/*
 * Following Groovy code is used to dynamically set the timeout for this Jenkins job.
 *
 * If this is a release job, in the sense that it was triggered from Jenkins by the 'Perform Maven Release'
 * the default timeout will be increased by the "increaseBuildTime" variable.
 * Reason is that a release will build both the release version and the next development version. It will also
 * build and publish the documentation (maven site) for the project.
 */
// from http://stackoverflow.com/a/32704455

// get current thread / Executor and current build
Thread thr = Thread.currentThread()
AbstractBuild build = thr?.executable

try {
    String release_version_value = build.buildVariableResolver.resolve("MVN_RELEASE_VERSION")

    //Replace %DEFAULT_TIMEOUT% by current timeout
    int defaultTimeout = %DEFAULT_TIMEOUT%

    if (release_version_value != null) {
        int increaseBuildTime = 4
        defaultTimeout = defaultTimeout * increaseBuildTime

        println "Release build, increase the timeout times " + increaseBuildTime
    }

    Map mapVar = [:]
    mapVar['JOB_TIMEOUT'] = defaultTimeout

    println "Timeout set to \${JOB_TIMEOUT}: " + mapVar['JOB_TIMEOUT']

    return mapVar

} catch (Throwable t) {
    println(t)
    throw t
}
