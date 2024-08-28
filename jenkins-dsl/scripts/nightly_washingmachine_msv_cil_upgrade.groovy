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

String washingmachineList = '{TPG}_nightly_washingmachine_list'
File file = new File("/proj/eta-automation/jenkins/kascmadm/job_config/", washingmachineList)
XmlParser parser = new XmlParser()
groovy.util.Node clusters = null
result = Result.SUCCESS

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
    return false
}

(disabledClusters, disabledClusterHosts) = disableWashingmachines(clusters) // Disable all running Washingmachines through their on-off job
startMsvCilCompareJobs(clusters)                                            // Starts all msv/cil compare jobs
reEnableWashingmachines(disabledClusters, disabledClusterHosts)             // Restarts all Washingmachines previously disabled through their on-off job
Thread.currentThread().executable.setResult(result)

def disableWashingmachines(groovy.util.Node clusters) {
    println ("---- Disabling Washingmachines ----")

    ArrayList disabledClusters = new ArrayList()
    ArrayList disabledClusterHosts = new ArrayList()

    ArrayList jobs = new ArrayList()
    ArrayList futures = new ArrayList()
    ArrayList buildParameters = new ArrayList()
    ArrayList currentRetries = new ArrayList()
    String lastBranch = ""

    // Go through each cluster
    clusters.eachWithIndex { it, index ->

        // If the current iterator is null, skip it
        // They will be null when the job is finished with them, nulling them to keep
        // the correct index
        if (!it) {
            return
        }

        // Only start one _onoff job
        if (lastBranch == it.@branch) {
            println ("INFO: ${it.@branch} branch's onoff job already started. Skipping")
            return
        }

        (job, future, parameters) = triggerTurnOffJob(it, disabledClusterHosts)

        // If we're unable to either read the properties file or couldn't start/find the job
        if (!job && !parameters) {
            println ("ERROR: Skipping cluster")
            result = Result.UNSTABLE
            return
        }

        // The job wasn't created (The WM was already turned off)
        if (!future) {
            return
        }

        jobs.push(job)
        futures.push(future)
        buildParameters.push(parameters)
        currentRetries.push(0)
        disabledClusters.push(it)
        lastBranch = it.@branch

    }

    // Go through all created on-off jobs and wait until they've finished
    while (true) {
        if (isAllJobsFinished(jobs, futures, buildParameters, currentRetries, 3)) {
            println ("INFO: All onoff jobs are finished.\n\n")
            break
        }
        safeJenkinsWait(30000)
    }

    return [disabledClusters, disabledClusterHosts]
}

def triggerTurnOffJob(groovy.util.Node node, ArrayList disabledClusterHosts) {
    println ("--- Turn Washingmachine Off ---")

    Properties properties = readProperties(node.@branch)
    if (!properties) {
        return [null, null, null]
    }

    String jobName = "{TPG}_washingmachine"
    if (node.@branch && node.@branch != "master") {
        jobName += ("_" + node.@branch)
    }
    jobName += "_onoff"

    FreeStyleProject job = Hudson.instance.getJob(jobName)
    if (!job) {
        println ("ERROR: Could not find job '${jobName}'")
        return [null, null, null]
    }

    if (properties.JOB_ENABLED == "true") {
        // Save which jobs we're turning off so we can enable them again
        disabledClusterHosts.push(properties.HOST_SET)

        ArrayList params = [
            new StringParameterValue('ACTION', 'Disable'),
            new StringParameterValue('ENABLED_HOSTS', 'none'),
            new StringParameterValue('REASON', 'Disabled WM for MSV and CIL upgrade.')
        ]

        // Go through all parameters from the onoff job.
        // If The parameter exist in the params.properties file it will be added.
        // containing the values from the params.properties file
        // This makes sure no other settings is changed in the params.properties file
        ParametersDefinitionProperty prop = job.getProperty(ParametersDefinitionProperty.class)
        if (prop) {
            for (param in prop.getParameterDefinitions()) {
                String paramName = param.getName()
                if (properties.stringPropertyNames().contains(paramName)) {
                    params.add(new StringParameterValue(paramName, properties.getProperty(paramName)))
                }
            }
        }

        (future, buildParameters) = createJob(job, params, 30000)
        return [job, future, buildParameters]
    }
    else {
        println ("INFO: Cluster's Washingmachine already disabled")
    }
    return [job, null, null]
}

def startMsvCilCompareJobs(groovy.util.Node clusters) {
    println ("---- Starting MSV and CIL comparison jobs ----")

    ArrayList clustersList = new ArrayList()
    ArrayList jobs = new ArrayList()
    ArrayList futures = new ArrayList()
    ArrayList buildParameters = new ArrayList()
    ArrayList currentRetries = new ArrayList()
    int noJobsStarted = 0
    boolean success = true

    // Convert the clusters from Node to ArrayList
    clusters.each {
        clustersList.push(it)
    }
    // Go through all clusters and start msv_cil_compare jobs
    // Then wait for all of them to finish
    boolean breakWhileLoop = false
    int skippedClusters = 0
    while(true) {

        clustersList.eachWithIndex { it, index ->
            if (!it || breakWhileLoop) {
                return
            }

            // Get the washingmachine (makes sure it exist)
            FreeStyleProject job = getWashingmachine(it)
            if (!job) {
                println ("ERROR: Skipping cluster")
                clustersList.set(index, null)
                skippedClusters++
                success = false
                result = Result.UNSTABLE
                return
            }

            // Wait for the clusters washingmachine to finish its job
            if (!isWashingmachineFree(job)) {
                return
            }

            (job, future, parameters) = triggerMsvCilCompareJob(it)

            // If we couldn't read the properties file, or start/find the job
            if (!job || !parameters) {
                breakWhileLoop = true
                success = false
                return
            }

            noJobsStarted++
            jobs.push(job)
            futures.push(future)
            buildParameters.push(parameters)
            currentRetries.push(0)
            clustersList.set(index, null)
            println ("")
        }

        if (breakWhileLoop) {
            break
        }

        // Wait for all started jobs to finish
        if ((noJobsStarted >= clustersList.size() - skippedClusters) &&
            isAllJobsFinished(jobs, futures, buildParameters, currentRetries, 3)) {
            println ("INFO: All MSV and CIL upgraded are finished\n\n")
            break
        }
        safeJenkinsWait(300000)
    }

    return success
}

def getWashingmachine(groovy.util.Node node) {
    println ("--- Get the washingmachine for branch '${node.@branch}' ---")

    String washingmachine = "{TPG}_washingmachine"
    if (node.@branch && node.@branch != "master") {
        washingmachine += ("_" + node.@branch)
    }

    FreeStyleProject job = Hudson.instance.getJob(washingmachine)
    if (!job) {
        println ("ERROR: Could not get job '${washingmachine}. Skipping cluster!")
        return null
    }
    return job
}

def isWashingmachineFree(FreeStyleProject job) {
    println ("--- Checking if Washingmachine '${job.fullDisplayName}' is finished ---")

    if (job.isInQueue()) {
        println ("INFO: Removing a '$HyperlinkNote.encodeTo('/' + job.url, job.fullDisplayName)' build from the queue")
        Jenkins.instance.queue.cancel(job.getQueueItem())
    }

    if (job.isBuilding()) {
        println ("INFO: A '${HyperlinkNote.encodeTo('/' + job.url, job.fullDisplayName)}' is still building")
        return false
    }

    println ("INFO: Job '${HyperlinkNote.encodeTo('/' + job.url, job.fullDisplayName)}' is free")
    return true
}

def triggerMsvCilCompareJob(groovy.util.Node node) {
    println ("--- Create A New Compare MSV and CIL for '${node.@branch}' branch ---")

    Properties properties = readProperties(node.@branch)
    if (!properties) {
        return [null, null, null]
    }

    FreeStyleProject job = Hudson.instance.getJob('compare_and_upgrade_msv_cil_versions')
    if (!job) {
        println ("ERROR: Could not find job 'compare_and_upgrade_msv_cil_versions'")
        return [null, null, null]
    }

    String fileName = "{TPG}_washingmachine"
    if (node.@branch && node.@branch != "master") {
        fileName += ("_" + node.@branch)
    }

    String forceInstallation = build.environment.get("FORCE_MSV_CIL_INSTALLATION")

    ArrayList params = [
        new StringParameterValue('MSV', node.@msv),
        new StringParameterValue('CIL', node.@cil),
        new StringParameterValue('VMAPI_PREFIX', node.@vmapiprefix),
        new StringParameterValue('PRODUCT', '{TPG}'),
        new StringParameterValue('INSTALLATIONTYPE', node.@installationtype),
        new StringParameterValue('BUILD_USER_EMAIL', node.@usermail),
        new StringParameterValue("MSVCIL_SETTINGS", ("/proj/eta-automation/jenkins/kascmadm/job_config/" +
                                                      fileName + "_params.properties")),
        new StringParameterValue("FORCEINSTALLATION", forceInstallation),
        new StringParameterValue("MSV_RESOURCE_PROFILE", "{MSV_RESOURCE_PROFILE}"),
        new StringParameterValue("CIL_RESOURCE_PROFILE", "{CIL_RESOURCE_PROFILE}")
    ]

    (future, buildParameters) = createJob(job, params, 180000)
    return [job, future, buildParameters]
}

def reEnableWashingmachines(ArrayList clusters, ArrayList hosts) {
    println ("---- Enabling washingmachines ----")

    ArrayList jobs = new ArrayList()
    ArrayList futures = new ArrayList()
    ArrayList buildParameters = new ArrayList()
    ArrayList currentRetries = new ArrayList()


    clusters.eachWithIndex { it, index ->
        if (!it) {
            return
        }

        (job, future, parameters) = triggerTurnOnJob(it, hosts[index])
        if (!job && !parameters) {
            result = Result.UNSTABLE
            clusters.set(index, null)
            println ("ERROR: Skipping cluster")
            return
        }
        jobs.push(job)
        futures.push(future)
        buildParameters.push(parameters)
        currentRetries.push(0)
        println ("\n")
    }

    while (true) {
        if (isAllJobsFinished(jobs, futures, buildParameters, currentRetries, 3)) {
            println ("INFO: All onoff jobs are finished.\n\n")
            break
        }
        safeJenkinsWait(30000)
    }
}

def triggerTurnOnJob(groovy.util.Node node, String hostSet) {
    println ("--- Turn Washingmachine On ---")

    Properties properties = readProperties(node.@branch)
    if (!properties) {
        return [null, null, null]
    }

    String jobName = "{TPG}_washingmachine"
    if (node.@branch && node.@branch != "master") {
        jobName += ("_" + node.@branch)
    }
    jobName += "_onoff"

    FreeStyleProject job = Hudson.instance.getJob(jobName)
    if (!job) {
        println ("ERROR: Could not find job '${jobName}'")
        return [null, null, null]
    }

    ArrayList params = [
        new StringParameterValue('ACTION', 'Enable'),
        new StringParameterValue('ENABLED_HOSTS', hostSet),
        new StringParameterValue('REASON', 'Re-enabled WM after MSV and CIL upgrade.')
    ]

    // Go through all parameters from the onoff job.
    // If The parameter exist in the params.properties file it will be added.
    // containing the values from the params.properties file
    // This makes sure no other settings is changed in the params.properties file
    ParametersDefinitionProperty prop = job.getProperty(ParametersDefinitionProperty.class)
    if (prop) {
        for (param in prop.getParameterDefinitions()) {
            String paramName = param.getName()
            if (properties.stringPropertyNames().contains(paramName)) {
                params.add(new StringParameterValue(paramName, properties.getProperty(paramName)))
            }
        }
    }

    (future, buildParameters) = createJob(job, params, 30000)
    return [job, future, buildParameters]
}

def isAllJobsFinished(ArrayList jobs, ArrayList futures, ArrayList parameters,
                      ArrayList currentRetries, int maxRetries ) {
    println ("--- Checking If All Jobs Is Finished ---")

    int jobsRemaining = 0
    futures.eachWithIndex { it, index ->
        if (!it) {
            return
        }

        if (it.isCancelled()) {
            println ("INFO: A '${HyperlinkNote.encodeTo('/' + jobs[index].url, jobs[index].fullDisplayName)}' job was aborted.")
            futures.set(index, null)
        }
        else if (!it.isDone()) {
            println ("INFO: A '${HyperlinkNote.encodeTo('/' + jobs[index].url, jobs[index].fullDisplayName)}' job is not finished yet.")
            jobsRemaining++
        }
        else if (it.isDone()) {
            Result result = it.get().result
            if (result == Result.FAILURE || result == Result.UNSTABLE) {
                if (currentRetries[index] < maxRetries) {
                    futures.set(index, jobs[index].scheduleBuild2(0, new Cause.UpstreamCause(build),
                                                                  parameters[index]))
                    jobsRemaining++
                    currentRetries[index]++
                    println ("INFO: A '${HyperlinkNote.encodeTo('/' + jobs[index].url, jobs[index].fullDisplayName)}' job failed." +
                             " Retrying (${currentRetries[index]}/${maxRetries}).")
                }
                else {
                    println ("INFO: A '${HyperlinkNote.encodeTo('/' + jobs[index].url, jobs[index].fullDisplayName)}' job failed " +
                             " after ${currentRetries[index]} retries.")
                    result = Result.UNSTABLE
                    futures.set(index, null)
                }
            }
            else if (result == Result.ABORTED) {
                println ("INFO: A '${HyperlinkNote.encodeTo('/' + jobs[index].url, jobs[index].fullDisplayName)}' job was aborted.")
                futures.set(index, null)
            }
            else {
                println ("INFO: A '${HyperlinkNote.encodeTo('/' + jobs[index].url, jobs[index].fullDisplayName)}' job was finished.")
                futures.set(index, null)
            }
        }
    }

    if (jobsRemaining == 0) {
        println ("INFO: All job are finished")
        return true
    }

    println ("INFO: ${jobsRemaining} jobs remaining\n")
    return false
}

def createJob(FreeStyleProject job, ArrayList params, int sleepTime, ArrayList deny = new ArrayList()) {
    println ("-- Creating a new '${HyperlinkNote.encodeTo('/' + job.url, job.fullDisplayName)}' job --")

    ArrayList defaultParameters = new ArrayList()
    ParametersDefinitionProperty prop = job.getProperty(ParametersDefinitionProperty.class)
    FutureImpl future = null
    ParametersAction combinedParams = null
    if (prop) {
        println ("INFO: Parameters found for current job:")
        for(param in prop.getParameterDefinitions()) {
            try {
                ParameterValue defaultValue = param.getDefaultParameterValue()
                if (defaultValue != null) {
                    String name = defaultValue.getName()
                    if (!(name in deny)) {
                        String value = defaultValue.createVariableResolver(null).resolve(name)
                        println ("INFO: ${name}: ${value}")
                        defaultParameters.push(defaultValue)
                    }
                    else {
                        println ("INFO: Deny - Name: ${name}")
                    }
                }
                else {
                    println ("WARNING: Could not get default value for parameter '${defaultValue}'")
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
        println ("INFO: A '${HyperlinkNote.encodeTo('/' + job.url, job.fullDisplayName)}' job was created")
        safeJenkinsWait(sleepTime)
    }
    catch (CancellationException e) {
        throw new AbortException("ERROR: ${job.fullDisplayName} Aborted")
    }
    return [future, combinedParams]
}

def readProperties(String branch) {
    println ("-- Reading properties for branch '${branch}' --")

    String fileName = "{TPG}_washingmachine"
    if (branch && branch != "master") {
        fileName += ("_" + branch)
    }

    Properties properties = new Properties()

    File propertiesFile = new File("/proj/eta-automation/jenkins/kascmadm/job_config/", fileName +
                                    "_params.properties")
    if (!propertiesFile.exists()) {
        println ("ERROR: Failed to read '${propertiesFile}'")
        return null
    }

    propertiesFile.withInputStream {
        properties.load(it)
    }
    return properties
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

def safeJenkinsWait(int timeTowait) {
    // This makes it possible to abort the build even when it is waiting
    Executor executor = getBinding().getVariable('build').getExecutor()
    synchronized (executor) {
       executor.wait(timeTowait)
    }
}
