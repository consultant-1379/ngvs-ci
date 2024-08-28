import groovy.json.JsonSlurper
import java.net.URLEncoder
import jenkins.model.Jenkins
import hudson.model.*;
import hudson.model.queue.FutureImpl
import hudson.console.HyperlinkNote
import hudson.AbortException
import hudson.FilePath
import hudson.model.Result
import hudson.remoting.Channel
import java.util.concurrent.CancellationException

// Create a File object representing the file '<path><file>'
// {TPG} will be replaced to the current TPG building by the DSL
String washingmachineList = '{TPG}_nightly_list'
File file = new File("/proj/eta-automation/jenkins/kascmadm/job_config/", washingmachineList)
XmlParser parser = new XmlParser()
result = Result.SUCCESS
groovy.util.Node clusters = null

if (!file.exists()) {
    file.createNewFile()
    StringWriter sw = new StringWriter()
    groovy.xml.MarkupBuilder xml = new groovy.xml.MarkupBuilder(sw)
    xml.clusters() { }
    FileWriter fw = new FileWriter(file)
    fw.write(sw.toString())
    fw.close()
}

try {
    clusters = parser.parse(file)
}
catch (Exception e) {
    println ("ERROR: " + e)
    return 1
}

msvCilJob = null
msvCilBuildParameters = new ArrayList()
msvCilCurrentRetries = new ArrayList()
msvCilFutures = new ArrayList()
msvCilFutureToNode = new ArrayList()

if (!createJobs(clusters)) {
    return 1
}

waitForJobs()
Thread.currentThread().executable.setResult(result)
return 0

def createJobs(groovy.util.Node clusters) {
    println ("---- Creating Jobs ----")
    clusters.each {
        if (it.@installationtype && it.@installationtype != "tpg") {
            (job, future, parameters) = createMsvCilJob(it)

            if (!job && !parameters) {
                result = Result.UNSTABLE
                return
            }

            msvCilJob = job
            msvCilBuildParameters.push(parameters)
            msvCilFutures.push(future)
            msvCilCurrentRetries.push(0)
            msvCilFutureToNode.push(it)
            println ("")

        }
        else {
            (job, future, parameters) = createInstallTargethostJob(it)
            if (!job || !parameters) {
                return
            }
        }
    }
    return true
}

def waitForJobs() {
    println ("---- Waiting for MSV and CIL upgrades to finish ----")

    if (msvCilFutures.size() == 0) {
        println ("INFO: No MSV/CIL upgrades to wait for")
        return
    }

    String jobUrl = HyperlinkNote.encodeTo('/' + msvCilJob.url, msvCilJob.fullDisplayName)
    while(true) {
        int jobsRemaining = 0
        msvCilFutures.eachWithIndex { it, index ->
            if (!it) {
                msvCilFutures.set(index, null)
                return
            }

            if (isJobFinished(msvCilJob, msvCilFutures[index])) {
                // If the current job is finished

                Result result = it.get().result
                if (result == Result.FAILURE || result == Result.UNSTABLE) {
                    // Finished but failed or is unstable

                    if (doRestartFinishedJob(msvCilJob, msvCilFutures,
                                             msvCilBuildParameters, msvCilCurrentRetries,
                                             index, 3)) {
                        // If the job have failed less than maxRetries and was restarted
                        jobsRemaining++
                    }
                    else {
                        // If the failed/unstable job already has been started 3 times
                        msvCilFutures.set(index, null)
                    }
                }
                else if (result == Result.ABORTED) {
                    // Finished but aborted
                    println ("INFO: A '" + jobUrl + "' job was aborted")
                    msvCilFutures.set(index, null)
                }
                else if (result == Result.NOT_BUILT) {
                    // Finished but not built. This might occur when there was a problem in an
                    // earlier stage which prevented this stage from building. However unclear if
                    // this will ever occur at this stage (after the job is finished).
                    if (doRestartFinishedJob(msvCilJob, msvCilFutures,
                                             msvCilBuildParameters, msvCilCurrentRetries,
                                             index, 3)) {
                        jobsRemaining++
                    }
                }
                else {
                    // Finished successful
                    println ("INFO: A '" + jobUrl + "' job finished successfully")
                    msvCilFutures.set(index, null)
                    createInstallTargethostJob(msvCilFutureToNode[index])
                }
            }
            else {
                // Not finished yet
                jobsRemaining++
            }
        }

        if (jobsRemaining == 0) {
            break
        }

        println ("Waiting for ${jobsRemaining} jobs to finish..")
        safeJenkinsWait(300000)
    }
}

def createInstallTargethostJob(groovy.util.Node node) {
    println ("--- Creating a Targethost Install job ---")

    Properties properties = readProperties()
    if (!properties) {
        return [null, null, null]
    }

    FreeStyleProject job = Hudson.instance.getJob('{TPG}_targethost_install')
    if (!job) {
        println ("ERROR: Could not find job '{TPG}_targethost_install'")
        return [null, null, null]
    }

    ArrayList params = [
        new StringParameterValue('RETRIES', '2'),
        new StringParameterValue('USERMAIL', node.@usermail),
        new StringParameterValue('VMAPI_PREFIX', node.@vmapiprefix),
        new StringParameterValue('VERSION', 'LATEST'),
        new StringParameterValue('JIVE_VERSION', 'LATEST')
    ]

    if (node.@msv) {
        params.add(new StringParameterValue('MSV', node.@msv))
    }

    if (node.@cil) {
        params.add(new StringParameterValue('CIL', node.@cil))
    }

    if (node.@resourceprofile) {
        params.add(new StringParameterValue('RESOURCE_PROFILE', node.@resourceprofile))
    }

    // This will be replace with the map-pairs defined when calling the 'NightlyTargethostInstallJobBuilder'
    // class. It will be replaced by the 'targethostInstallParameters' map defined in the constructor.
    // Ex: super.createNightlyFullTargethostInstall(1, ["TARGETHOST":"targethost"])
    // will result in params.push(new StringParameterValue('TARGETHOST', node.@targethost))
    {TPG_TARGETHOST_PARAMETERS}

    (future, buildParameters) = createJob(job, params, 180000)
    return [job, future, buildParameters]
}

def readProperties() {
    println ("-- Reading properties --")

    Properties properties = new Properties()

    File propertiesFile = new File("/proj/eta-automation/jenkins/kascmadm/job_config/" +
                                   "{TPG}_washingmachine_params.properties")
    if (!propertiesFile.exists()) {
        println ("ERROR: Failed to read '${propertiesFile}'")
        return null
    }

    propertiesFile.withInputStream {
        properties.load(it)
    }
    return properties
}

def isJobFinished(FreeStyleProject job, FutureImpl future) {
    if (future.isCancelled()) {
        println ("INFO: A '${HyperlinkNote.encodeTo('/' + job.url, job.fullDisplayName)}'" +
                 " job was aborted")
        return true
    }
    else if (!future.isDone()) {
        return false
    }

    return true
}

def doRestartFinishedJob(FreeStyleProject job, ArrayList futures, ArrayList parameters,
                         ArrayList currentRetries, int index, int maxRetries) {
    println ("--- Checking if a failed job should be restarted ---")

    if (currentRetries[index] < maxRetries) {
        futures.set(index, job.scheduleBuild2(0, new Cause.UpstreamCause(build), parameters[index]))
        currentRetries[index]++
        println ("INFO: A '${HyperlinkNote.encodeTo('/' + job.url, job.fullDisplayName)}' " +
                 "job failed. Retrying (${currentRetries[index]}/${maxRetries})")
    }
    else {
        println ("WARNING: A '${HyperlinkNote.encodeTo('/' + job.url, job.fullDisplayName)}' " +
                 "job failed after ${currentRetries[index]} retries")
        result = Result.UNSTABLE
        return false
    }
    return true
}

def createJob(FreeStyleProject job, ArrayList params, int timeToWait) {
    println ("-- Creating a new '${HyperlinkNote.encodeTo('/' + job.url, job.fullDisplayName)}' " +
             "job --")

    ArrayList defaultParameters = new ArrayList()
    ParametersDefinitionProperty prop = job.getProperty(ParametersDefinitionProperty.class)
    FutureImpl future = null
    ParametersAction combinedParams = null
    if (prop) {
        println ("INFO: Parameters found for current job:")
        for(param in prop.getParameterDefinitions()) {
            try {
                ParameterValue defaultValue = param.getDefaultParameterValue()
                if (defaultValue) {
                    String name = defaultValue.getName()
                        String value = defaultValue.createVariableResolver(null).resolve(name)
                        println ("INFO: ${name}: ${value}")
                        defaultParameters.push(defaultValue)
                    }
                else {
                    println ("WARNING: Could not get default value for parameter " +
                             "'${defaultValue}'")
                }
            }
            catch (Exception e) {
                println ("ERROR: Fail: ${param.name} e: ${e}")
            }
        }
        print("\n")
    }

    try {
        combinedParams = combineBuildParameters(params, defaultParameters)
        future = job.scheduleBuild2(0, new Cause.UpstreamCause(build), combinedParams)
        println ("INFO: A '${HyperlinkNote.encodeTo('/' + job.url, job.fullDisplayName)}' " +
                 "job was created")
        safeJenkinsWait(timeToWait)
    }
    catch (CancellationException e) {
        throw new AbortException("ERROR: ${job.fullDisplayName} Aborted")
    }
    return [future, combinedParams]
}

def createMsvCilJob(groovy.util.Node node) {
    println ("--- Creating a MSV CIL Compare job ---")

    Properties properties = readProperties()
    if (!properties) {
        return [null, null, null]
    }

    FreeStyleProject job = Hudson.instance.getJob('compare_and_upgrade_msv_cil_versions')
    if (!job) {
        println ("ERROR: Could not find job 'compare_and_upgrade_msv_cil_versions'")
        return [null, null, null]
    }

    String forceInstallation = build.environment.get("FORCE_MSV_CIL_INSTALLATION")

    ArrayList params = [
        new StringParameterValue('MSV', node.@msv),
        new StringParameterValue('CIL', node.@cil),
        new StringParameterValue('VMAPI_PREFIX', node.@vmapiprefix),
        new StringParameterValue('PRODUCT', '{TPG}'),
        new StringParameterValue('INSTALLATIONTYPE', node.@installationtype),
        new StringParameterValue('BUILD_USER_EMAIL', node.@usermail),
        new StringParameterValue("MSVCIL_SETTINGS", "/proj/eta-automation/jenkins/kascmadm/job_config/" +
                                                    "{TPG}_washingmachine_params.properties"),
        new StringParameterValue("FORCEINSTALLATION", forceInstallation),
        new StringParameterValue("MSV_RESOURCE_PROFILE", "{MSV_RESOURCE_PROFILE}"),
        new StringParameterValue("CIL_RESOURCE_PROFILE", "{CIL_RESOURCE_PROFILE}")
    ]

    (future, buildParameters) = createJob(job, params, 180000)
    return [job, future, buildParameters]
}

def combineBuildParameters(ArrayList userParams, ArrayList defaultParams) {
    println ("- Combining build parameters -")
    ArrayList combinedParams = new ArrayList()

    // Add user parameters (if the parameter exist in the first place)
    println ("INFO: Parameters provided by user:")
    userParams.each {
        println ("INFO: ${it.name}: ${it.value}")
        combinedParams << it
    }
    print("\n")

    // Adds all default parameters which wasn't altered by the user params
    defaultParams.each {
        boolean addMissingDefault = true
        combinedParams.each { combParam ->
            if (it.name == combParam.name) {
                addMissingDefault = false
            }
        }
        if (addMissingDefault) {
            println ("WARNING: Missing user parameter: ${it.name}")
            combinedParams << it
        }
    }
    println ("\nINFO: Combined parameter for current build:")
    for (param in combinedParams) {
        println ("INFO: ${param.name}: ${param.value}")
    }
    print("\n")
    return new ParametersAction(combinedParams)
}

def safeJenkinsWait(int timeToWait) {
    // This makes it possible to abort the build even when it is waiting
    Executor executor = getBinding().getVariable('build').getExecutor()
    synchronized (executor) {
       executor.wait(timeToWait)
    }
}
