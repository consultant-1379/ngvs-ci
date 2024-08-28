import hudson.FilePath

String projectName = new String("${project_name}")
String formattedProjectName = projectName.toLowerCase().replaceAll(/\.|-|_|bssf/, "")

createDisableJobsFileIfNeeded()

projects = getClassNames()
projects.each {
    project = it
    if (formattedProjectName != project.toLowerCase()) {
        println("${formattedProjectName} does not match ${project.toLowerCase()}. Continue on next.")
        return
    }
    def instance = loadProjectClass(project)
    runProjectClass(projectName, instance)
}

cleanupDisableJobsFileIfNeeded()

private List<String> getClassNames() {
    def build = Thread.currentThread().executable
    workspace = build.workspace.toString()
    projectdir = workspace +
            "/src/main/groovy/com/ericsson/bss/project/"
    println("projects directory: " + projectdir)

    channel = build.workspace.channel
    // TODO: hudson.FilePath needs to be mocked somehow when running CLI smoketests.
    fp = new hudson.FilePath(channel, workspace +
            "/src/main/groovy/com/ericsson/bss/project/")

    projects = fp.list()
            .findAll { it.getName().endsWith('.groovy') }
            .collect { it.getName()[0..-8] }
    println("projects: " + projects)
    return projects
}

private Object loadProjectClass(project) {
    println("Load project class: " + project)
    def instance = this.class.classLoader.loadClass('com.ericsson.bss' +
            '.project.' + project, true, false)?.newInstance()
    return instance
}

private void runProjectClass(projectName, instance) {
    println("Run project class: " + projectName)
    boolean isProject = instance.metaClass.respondsTo(instance, 'runProject', String)
    if (isProject) {
        if (instance.runProject(projectName)) {
            println("Will create dsl for project: " + projectName)
            instance.create(this)
        } else {
            println("Loaded class instance (" + instance.getClass().getName() +
                    ") does not match project name. Continue on next.")
        }
    } else {
        println("!No project instance to run!")
    }
}

private void createDisableJobsFileIfNeeded() {
    try {
        if ('true'.equalsIgnoreCase("${ALL_DSL_JOBS_DISABLED}")) {
            FilePath disableJobsFile = getDisableJobsFilePath()
            try {
                disableJobsFile.touch(Calendar.instance.timeInMillis)
                if (!disableJobsFile.exists()) {
                    throw new Exception("Unable to create file.")
                }
                println("File created at: " + disableJobsFile.getRemote())
            } catch (IOException e) {
                println("Unable to create file, due to: " + e)
                throw e
            }
        } else {
            cleanupDisableJobsFileIfNeeded()
        }
    } catch (MissingPropertyException ignore) {
        // The ALL_DSL_JOBS_DISABLED variable does not exist in the Jenkins
        // server. Thus the exception itself is ignored but the cleanup
        // needs to be done
        cleanupDisableJobsFileIfNeeded()
    }
}

private void cleanupDisableJobsFileIfNeeded() {
    FilePath disableJobsFile = getDisableJobsFilePath()
    try {
        if (disableJobsFile.exists()) {
            disableJobsFile.delete()
        }
    } catch (IOException e) {
        println("Unable to remove file, due to: " + e)
        throw e
    }
}

private FilePath getDisableJobsFilePath() {
    def build = Thread.currentThread().executable
    if (build.workspace.isRemote()) {
        def channel = build.workspace.channel
        return new FilePath(channel, build.workspace.toString() + '/disableJobs')
    } else {
        return new FilePath(new File(build.workspace.toString() + '/disableJobs'))
    }
}