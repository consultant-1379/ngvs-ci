package com.ericsson.bss.job.washingmachine

import static Templates.ONOFF_SET_PROPERTIES_TEMPLATE
import static Templates.ONOFF_GET_PROPERTIES_TEMPLATE

import com.ericsson.bss.AbstractJobBuilder
import com.ericsson.bss.util.Email
import javaposse.jobdsl.dsl.Job

import groovy.text.SimpleTemplateEngine
import groovy.text.Template

class Templates {
    static final String ONOFF_SET_PROPERTIES_TEMPLATE = '''
        import hudson.model.Item
        import jenkins.model.*

        Jenkins instance = Jenkins.getInstance()

        Map propertyMap = [:]
        <%
        properties.each {
            println "propertyMap['\$it'] = build.buildVariableResolver.resolve('\$it')"
        }
        %>

        String jobEnabled = build.buildVariableResolver.resolve("JOB_ENABLED")
        String reason = build.buildVariableResolver.resolve("REASON")
        String user = build.getEnvironment(listener).get("BUILD_USER_ID")
        String jobName = "$KeepaliveProjectName"
        Item job = instance.getItem(jobName)

        if (jobEnabled == 'Disable') {
            println 'Disabling ' + jobName
            desc = 'Job disabled by ' + user + ' with reason: ' + reason + ''
            println 'With description: ' + desc
            job.disable()
            job.setDescription(desc)
        } else {
            String hostSetDescription = 'all hosts'

            println 'Enabling ' + jobName
            desc = 'Job enabled (for ' + hostSetDescription + ') by ' + user + ' with reason: ' + reason + ''
            println 'With description: ' + desc
            job.enable()
            job.setDescription(desc)
        }

        prepareParams(jobEnabled == 'Enable', propertyMap)

        private void prepareParams(boolean jobEnabled, Map propertyMap) {
            Properties props = new Properties()
            File propsFile = new File('/proj/eta-automation/jenkins/kascmadm/job_config/${parentName}_params.properties')
            propsFile.getParentFile().mkdirs()
            propsFile.createNewFile()
            props.load(propsFile.newDataInputStream())

            propertyMap.each{ k, v ->
                props.setProperty("\\\$k", "\\\$v")
            }

            props.setProperty('JOB_ENABLED', Boolean.toString(jobEnabled))
            props.store(propsFile.newWriter(), null)

            println 'Content of the params file:'
            println propsFile.text
        }
    '''.stripIndent()

    static final String ONOFF_GET_PROPERTIES_TEMPLATE = '''
        version_list = []
        metadataPath = "$artifactUrl"

        try {
            files = metadataPath.tokenize(',')
            for (String file : files) {
                (path, fromversion) = file.tokenize(';')
                metadata = new XmlSlurper().parse(path + '/maven-metadata.xml')
                metadata.versioning.versions.version.each{
                    if (!fromversion || includeVersion(fromversion, it.text())) {
                        version_list.add(it.text())
                    }
                }
            }
        } catch (IOException e) {}

        version_list.sort{a, b-> a == b ? 0 : includeVersion(b, a) ? -1 : 1}

        File propsFile = new File("/proj/eta-automation/jenkins/kascmadm/job_config/${parentName}_params.properties")
        if (!propsFile.exists()) {
            return version_list
        }
        Properties props = new Properties()
        props.load(propsFile.newDataInputStream())

        storedValue = props.get("$propName")
        int index = 0
        version_list.find {
            if (it == storedValue) {
                version_list[index] = it + ':selected'
                return true
            }
            ++index
            return false
        }

        return version_list

        /*
         * The sorting algorith will first run a 2 part tokenizer
         * splitting away anything after a "-" for a string compare
         * and every "." for an integral compare.
         *
         * Each version part (major, minor, micro) is compared in
         * order and returns an early result on missmatch.
         * If all version parts are equal we run the string compare
         * if available (both version strings contain a "-") and
         * return the result.
         *
        */
        boolean includeVersion(String limitVer, String requestVer) {
            limitrcpart = ""
            requestrcpart = ""
            limitParts = ""
            requestParts = ""

            if (limitVer.contains("-")) {
                limitrcparts = limitVer.tokenize("-")
                limitrcpart = limitrcparts[1]
                limitParts = limitrcparts[0].tokenize(".")
            } else {
                limitParts = limitVer.tokenize(".")
            }

            if (requestVer.contains("-")) {
                requestrcparts = requestVer.tokenize("-")
                requestrcpart = requestrcparts[1]
                requestParts = requestrcparts[0].tokenize(".")
            } else {
                requestParts = requestVer.tokenize(".")
            }

            for (int i = 0; i < 3; i++) {
                try {
                    limitNum = limitParts[i].toInteger()
                    requestNum = requestParts[i].toInteger()

                    if (requestNum < limitNum) {
                        return false
                    }
                    else if (requestNum > limitNum) {
                        return true
                    }
                }
                catch (Exception e) {
                    return false
                }
            }
            /*
            * Make sure versions without strings get placed above those with.
            * ex. 2.2.0 is "larger" than 2.2.0-rcx.
            */
            if (limitrcpart.equals("")) {
                return false
            }
            if (requestrcpart.equals("")) {
                return true
            }
            /*
            * At this point we know both versions contains string
            * So we do an lexicographical string compare to determine
            * the "greater" string.
            */
            return requestrcpart.compareTo(limitrcpart) > 0
        }
    '''.stripIndent()
}

class WashingMachineUpgradeOnOffJobBuilder extends AbstractJobBuilder {
    protected out
    protected projectName
    protected boolean runXvfb = false

    private String recipient
    protected String suffix

    protected boolean isRpm = false

    protected ArrayList variantArtifacts

    public Job build() {
        jobName = projectName + '_washingmachine' + suffix + '_onoff'
        out.println("Creating washingmachine onoff job for " + projectName + '_washingmachine' + suffix)
        initProject(dslFactory.freeStyleJob(jobName))
        setProjectDescription(getProjectDescription())
        setRestrictLabel(RESTRICT_LABEL_MESOS_LIGHT)
        setJenkinsUserBuildVariables()
        addBuildParametersConfig()
        addBuildSteps()
        setBuildDescription('^.*With description: (.*)')
        super.configurePostBuildSteps().editableEmailNotification(getAlwaysMailConfig())
        return job
    }

    private void addBuildSteps() {
        job.with {
            steps { systemGroovyCommand(getGroovyCommand()) }
        }
    }

    private String getProjectDescription() {
        return 'A job to enable and disable ' + projectName + '_washingmachine' + suffix +
                ' to allow troubleshooting on targethosts.'
    }

    private String getProjectWithoutSuffix() {
        return projectName + '_washingmachine' + suffix
    }

    private String getProjectKeepaliveProjectName() {
        return projectName + '_washingmachine' + suffix + "_keepalive"
    }

    protected String getActionScript() {
        return dslFactory.readFileFromWorkspace('scripts/washingmachine/job_enabled_param.groovy').replace("<PROJECT_NAME>", getProjectWithoutSuffix())
    }

    protected void addBuildParametersConfig() {
        String toSingleLinePattern = ~/\s{2,}/
        String.metaClass.toSingleLine = {
            (delegate =~ toSingleLinePattern).replaceAll('')
        }
        Template template = new SimpleTemplateEngine().createTemplate(ONOFF_GET_PROPERTIES_TEMPLATE)
        job.with {
            parameters {
                out.println("variantArtifacts: " + variantArtifacts)
                variantArtifacts.each { variantArtifact ->
                    String groupid = variantArtifact.groupid.replaceAll('\\.', '/')
                    String artifactUrl = ""
                    variantArtifact.types.each { type ->
                        artifactUrl += """
                        https://arm.epk.ericsson.se/
                        artifactory/proj-${projectName}-${type}-local/
                        ${groupid}/${variantArtifact.artifactid},
                        """.toSingleLine()
                    }
                    LinkedHashMap binding = [
                        'artifactUrl' : artifactUrl,
                        'propName' : variantArtifact.name,
                        'parentName' : getProjectWithoutSuffix()
                    ]
                    String getPropsScript = template.make(binding)
                    activeChoiceParam("${variantArtifact.name}") {
                        description("${variantArtifact.desc}")
                        choiceType('SINGLE_SELECT')
                        groovyScript {
                            script(getPropsScript)
                            fallbackScript('return ["Error evaluating Groovy script."]')
                        }
                    }
                }
                activeChoiceParam("JOB_ENABLED") {
                    description("Choose to enable or disable " + getProjectWithoutSuffix())
                    choiceType("SINGLE_SELECT")
                    groovyScript {
                        script(getActionScript())
                        fallbackScript(defaultFallbackScript())
                    }
                }
                stringParam('REASON', '', 'The reason why you are changing state of ' + getProjectWithoutSuffix().capitalize() + ' WashineMachine.')
            }
        }
    }

    protected String getGroovyCommand() {
        Template template = new SimpleTemplateEngine().createTemplate(ONOFF_SET_PROPERTIES_TEMPLATE)
        LinkedHashMap binding = [
            'properties' : variantArtifacts.name,
            'KeepaliveProjectName' : getProjectKeepaliveProjectName(),
            'parentName' : getProjectWithoutSuffix()
        ]
        String shell = template.make(binding)
        return shell
    }

    protected String defaultFallbackScript() {
        return 'return ["Error evaluating Groovy script."]'
    }

    protected Email getAlwaysMailConfig() {
        return Email.newBuilder().withRecipient(recipient)
                .withSubject(getSubject())
                .withContent(getContent())
                .withAlwaysTrigger()
                .build()
    }

    private String getSubject() {
        String keepAliveProjectName = getProjectKeepaliveProjectName()
        return "${keepAliveProjectName} state changed to \$JOB_ENABLED"
    }

    private String getContent () {
        String keepAliveProjectName = getProjectKeepaliveProjectName()
        return """\
               ${keepAliveProjectName} WashingMachine Releasebranch \
               state changed to \$JOB_ENABLED by \$BUILD_USER_ID with reason '\$REASON'\
               """.stripIndent()
    }
}
