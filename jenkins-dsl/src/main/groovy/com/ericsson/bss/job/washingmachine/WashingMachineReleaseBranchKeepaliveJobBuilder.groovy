package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractJobBuilder

import javaposse.jobdsl.dsl.Job

public class WashingMachineReleaseBranchKeepaliveJobBuilder extends AbstractJobBuilder {

    protected static final int DAYS_TO_KEEP_BUILDS = 20
    protected static final int MAX_BUILDS_TO_KEEP = 20

    private def out
    private def projectName

    private Object releaseBranch

    private String fileName
    private String releaseBranchName
    private String washingmachineReleasebranchSuffix = '_washingmachine_releasebranch'
    private String rpmSuffix = ""
    private String rpmString = ""

    protected boolean useRpmWm = false
    protected boolean isRpm = false
    protected String buildTimer = 'H 1 * * *'

    public Job build() {
        if (isRpm) {
            rpmSuffix = '_rpm'
            rpmString = "Rpm "
        }
        releaseBranchName = releaseBranch.@releasebranchname
        Map properties = getPropertiesFromFile(fileName)
        jobName = getProjectName()
        out.println("Creating " + jobName + " for releasebranch" + releaseBranchName)
        initProject(dslFactory.freeStyleJob(jobName))
        setProjectDescription(getProjectDescription())
        job.with {
            logRotator(DAYS_TO_KEEP_BUILDS, MAX_BUILDS_TO_KEEP)

            blockOn(getBlockingProjects().tokenize(',')) {
                blockLevel('GLOBAL')
                scanQueueFor('ALL')
            }

            triggers {
                cron(buildTimer)
            }
        }
        setJenkinsUserBuildVariables()
        setRestrictLabel(RESTRICT_LABEL_MESOS_LIGHT)
        addBuildSteps()
        if (properties['JOB_ENABLED'] != null && !Boolean.parseBoolean(properties['JOB_ENABLED'])) {
            disable()
        } else {
            enable()
        }
        setBuildDescription('^.*With description: (.*)')
        return job
    }

    private void addBuildSteps() {
        if (useRpmWm) {
            job.with {
                steps {
                    systemGroovyCommand(getGroovyCommand())
                    conditionalSteps {
                        condition {
                            booleanCondition('$STATUS')
                        }
                        runner('Fail')
                        steps {
                            downstreamParameterized {
                                trigger(getProjectToBuild()) {
                                    parameters {
                                        propertiesFile(fileName)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else {
            job.with {
                steps {
                    downstreamParameterized {
                        trigger(getProjectToBuild()) {
                            parameters {
                                propertiesFile(fileName)
                            }
                        }
                    }
                }
            }
        }
    }

    private String getProjectDescription() {
        return 'A job to enable and disable ' + projectName.capitalize() + ' Washingmachine Releasebranch ' + rpmString + releaseBranchName +
                ' to allow troubleshooting on targethosts.'
    }

    private String getProjectName() {
        return projectName + washingmachineReleasebranchSuffix + rpmSuffix + "_" + releaseBranchName + "_keepalive"
    }

    private String getProjectToBuild() {
        return projectName + washingmachineReleasebranchSuffix + rpmSuffix + "_" + releaseBranchName
    }

    private String getParentBlockingJob() {
        return projectName + washingmachineReleasebranchSuffix + "_" + releaseBranchName
    }

    private String getBlockingProjects() {
        if (isRpm) {
            return projectName + washingmachineReleasebranchSuffix + "_" + releaseBranchName + ',' +
                   projectName + washingmachineReleasebranchSuffix + rpmSuffix + "_" + releaseBranchName
        }
        else {
            String block = projectName + washingmachineReleasebranchSuffix + "_" + releaseBranchName
            if (useRpmWm) {
                block += "," + projectName + washingmachineReleasebranchSuffix + "_rpm_" + releaseBranchName
            }
            return block
        }
    }

    private Map getPropertiesFromFile(String fileName) {
        checkIfFileExists(fileName)
        Properties properties = new Properties()
        FileReader fr = new FileReader(fileName)
        properties.load(fr)
        fr.close()
        return properties
    }

    private void checkIfFileExists(String fileName) {
        File propsFile = new File(fileName)
        if ( !propsFile.exists() ) {
            out.println("Creating " + fileName)
            propsFile.createNewFile()
        }
    }

    protected String getGroovyCommand() {
        return 'import jenkins.model.* \n' +
               'import hudson.model.*\n' +
               'Hudson instance = Jenkins.getInstance()\n' +
               'String jobname = "' + getParentBlockingJob() + '"\n' +
               'Item job = instance.getItem(jobname)\n' +
               'Build build = job.getLastCompletedBuild()\n' +
               'String result = build.getResult()\n' +
               'boolean STATUS=false\n' +
               'if (result == "SUCCESS" || result == "UNSTABLE") {\n' +
               '\tSTATUS=true\n' +
               '}\n' +
               'Map<String,String> env = System.getenv()\n' +
               'ParametersAction pa = new ParametersAction([\n' +
               '\t  new BooleanParameterValue("STATUS", STATUS)\n' +
               '])\n' +
               'Thread.currentThread().executable.addAction(pa)'
    }
}
