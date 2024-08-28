package com.ericsson.bss.job

import com.ericsson.bss.AbstractGerritTestJobBuilder
import javaposse.jobdsl.dsl.Job

public class GerritJiveTestJobBuilder extends AbstractGerritTestJobBuilder {

    protected String projectName
    String gerritDeployToKarafScript = ""

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addConfig()
        gerritTriggerSilent()
        return job
    }

    private void addConfig() {
        job.with {
            description(DSL_DESCRIPTION + getJobDescription())
            concurrentBuild()
            steps {
                systemGroovyCommand(dslFactory.readFileFromWorkspace('scripts/abortObsoletePatchSetBuild.groovy'))
                if (symlinkWorkspace) {
                    shell(symlinkMesosWorkSpace())
                }
                shell(gitInitiateRepository("\${WORKSPACE}"))
                shell(gitConfig("\${WORKSPACE}"))
                shell(getGitCache("\${WORKSPACE}"))
                shell(gitFetchGerritChange())
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))

                if (gerritName == ("jive/charging")) {
                    shell(getJiveChargingBuildCommand())
                }

                String deployToKarafScript = dslFactory
                        .readFileFromWorkspace(gerritDeployToKarafScript)
                        .replace("%JENKINS_JIVE_USER%", jenkinsJiveTestUser)

                shell(deployToKarafScript)
                if (projectName == ("rmca")) {
                    shell("export TIMESTAMP_DIR=`echo 20*`\n" +
                          "tar --ignore-failed-read -zcvf selenium_screenshots.tar.gz" +
                            " \$TIMESTAMP_DIR/selenium/build/reports/tests/*")
                }
            }

            Map variables = getInjectVariables()
            variables.remove("HOME")
            variables.put("SKIP_DIRTY_CLUSTER", "true")
            if (projectName == ("charging.access")) {
                variables.put("INSTANCE", "chargingaccess")
                variables.put("WITH_GERRIT_FEEDBACK", "false")
                variables.put("DOWNLOAD_JIVE_TESTS", "true")
            }
            else if (gerritName == ("jive/charging")) {
                variables.put("INSTANCE", "jivetest")
                variables.put("WITH_GERRIT_FEEDBACK", "true")
                variables.put("DOWNLOAD_JIVE_TESTS", "false")
            }
            else if (projectName == ("rmca")) {
                variables.remove("PATH")
                variables.put("CDT_INSTALL_FOLDER", getWorkspaceLocation())
                variables.put("FIREFOXDIR", "/proj/eta-tools/firefox/45.0esr/Linux_i386_64/firefox/firefox")
                variables.put("npm_config_cache", "\${WS_TMP}/npm_config_cache")
                variables.put("npm_config_prefix", "\${CDT_INSTALL_FOLDER}/node_modules")
                variables.put("PATH", "/proj/eta-tools/cdt/latest/node_modules/bin/:\${GIT_HOME}/bin:\${M2}:" +
                        "\${FIREFOXDIR}:\${npm_config_prefix}/bin/:/opt/local/dev_tools/nodejs/" +
                        "node-v0.10.20-linux-x64/bin/:\${PATH}")

                variables.put("INSTANCE", "rmca")
                variables.put("WITH_GERRIT_FEEDBACK", "true")
                variables.put("JAVA_TOOL_OPTIONS", JAVA_TOOL_OPTIONS)
            }
            else {
                variables.put("INSTANCE", "chargingcore")
                variables.put("WITH_GERRIT_FEEDBACK", "true")
                variables.put("DOWNLOAD_JIVE_TESTS", "true")
            }

            injectEnv(variables)
            addTimeoutConfig()
        }
    }

    @Override
    protected void gerritTriggerSilent() {
        job.with {
            triggers {
                gerrit {
                    project(gerritName, 'reg_exp:master')
                    configure {
                        (it / 'silentMode').setValue('true')
                        it / triggerOnEvents {
                            'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.events.PluginPatchsetCreatedEvent' {
                                excludeDrafts('true')
                                excludeNoCodeChange('true')
                            }
                            'com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.events.PluginDraftPublishedEvent' {
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void setPublishers() {
        job.with {
            publishers {
                textFinder('^.*Reporting end of session with status 2.*$', '', true, false, true)
            }
        }
        job.with {
            publishers {  buildDescription(/^.*JOB_DESCRIPTION(.*)/, '', /^.*JOB_DESCRIPTION(.*)/, '') }
        }

        if (projectName == ("rmca")) {
            job.with {
                publishers {
                    archiveArtifacts{ pattern('selenium_screenshots.tar.gz') }
                }
            }
        }

        job.with { publishers { wsCleanup() } }
    }

    @Override
    protected String gerritFeedbackSuccess(boolean verboseGerritFeedback) {
        return null
    }

    @Override
    protected String gerritFeedbackFail() {
        return null
    }

    protected String getJiveChargingBuildCommand() {
        return "mvn clean package -B -e -Dsurefire.useFile=false " +
            "--settings \${MAVEN_SETTINGS} -Dorg.ops4j.pax.url.mvn.settings=\${MAVEN_SETTINGS} " +
            "-Dmaven.repo.local=\${MAVEN_REPOSITORY} -Dorg.ops4j.pax.url.mvn.localRepository=\${MAVEN_REPOSITORY} " +
            "-Djava.io.tmpdir=\${WS_TMP}"
    }

    protected String getJobDescription() {
        String jobDescription = "" +
            "<i>This is an experimental job, for any questions or improvements ping exabbeh or" +
            " <a href='https://eta.epk.ericsson.se/helpdesk'>ETA helpdesk</a>.</i><br>\n" +
            "<br>\n"
        if (gerritName == ("jive/charging")) {
            jobDescription += "<h2>What is this?</h2>\n" +
                "1. Will build the jive test artifact from the code pushed to gerrit<br>\n" +
                "2. Runs jive tests towards the host<br>\n" +
                "<br>\n"
        }
        else if (projectName == ("rmca")){
            jobDescription += "<h2>What is this?</h2>\n" +
                    "1. Will build the code pushed to gerrit<br>\n" +
                    "2. Patches the targethost with the pushed code " +
                    "(<a href='https://eta.epk.ericsson.se/maven-sites/latest/com.ericsson.bss.tools/" +
                    "deploy-to-karaf/index.html'>deploy-to-karaf</a>).  " +
                    "The targethosts are " +
                    "<a href='https://eta.epk.ericsson.se/jenkins/job/rmca_deploy_to_karaf_install_rpm/'>" +
                    "continuously upgraded with the latest RPM</a> from arm.<br>\n" +
                    "3. Runs jive tests towards the host<br>\n" +
                    "4. The host will be cleaned and released into the pool<br>\n" +
                    "<br>\n" +
                    "<h3>What code will be tested?</h3>\n" +
                    "Will only install the modules that are specified in the <a " +
                    "href='https://arm" +
                    ".epk.ericsson.se/artifactory/proj-rmca-dev-local/com/ericsson/" +
                    "bss/rmca/compile/'>" +
                    "compile</a> for master branch on this repository and update GUI: <i>" + gerritName +
                    "</i><br><b>NOTE: Make sure your code is up to date, if it fails verify that you have re-based " +
                    "on latest master code.</b> <br>\n"
        }
        else {
            jobDescription += "<h2>What is this?</h2>\n" +
                "1. Will build the code pushed to gerrit<br>\n" +
                "2. Patches the targethost with the pushed code " +
                "(<a href='https://eta.epk.ericsson.se/maven-sites/latest/com.ericsson.bss.tools/" +
                "deploy-to-karaf/index.html'>deploy-to-karaf</a>).  " +
                "The targethosts are" +
                " <a href='https://internal.eta.epk.ericsson.se/jenkins/job/charging_deploy_to_karaf_install_rpm/'>" +
                "continuously upgraded with the latest RPM</a> from arm.<br>\n" +
                "3. Runs jive tests towards the host<br>\n" +
                "4. The host will be cleaned and released into the pool<br>\n" +
                "<br>\n" +
                "<h3>What code will be tested?</h3>\n" +
                "Will only install the modules that are specified in the <a " +
                "href='https://arm" +
                ".epk.ericsson.se/artifactory/proj-charging-dev-local/com/ericsson/" +
                "bss/rm/charging/productiondependencies/definition/'>" +
                "productiondependencies</a> for this repository: <i>" + gerritName +
                "</i><br>\n"
        }
        if (projectName == ("rmca")){
            jobDescription += "<h3>My tests failed for some unknown reason?</h3>\n" +
                    "This might be problems with your code or the fails have been introduced by " +
                    "someone else, compare results with <a href='https://tapas.epk.ericsson.se/#/suites/" +
                    "RMCA/RMCA%20Washingmachine'>Washingmachine</a> and " +
                    "<a href='https://tapas.epk.ericsson.se/#/suites/" +
                    "RMCA/RMCA%20Washingmachine%20RPM'>Washingmachine RPM</a> <br>\n" +
                    "Also check the <a href='https://eta.epk.ericsson.se/forums/viewforum.php?f=31'>forum "

        } else {
            jobDescription += "<h3>My tests failed for some unknown reason?</h3>\n" +
                "This might be problems with your code or the fails have been introduced by " +
                "someone else, compare results with <a href='https://tapas.epk.ericsson.se/#/suites/" +
                "Charging/Charging%20Washingmachine'>Washingmachine</a> and " +
                "<a href='https://tapas.epk.ericsson.se/#/suites/Charging/" +
                "Charging%20Washingmachine%20RPM'>Washingmachine RPM</a> <br>\n" +
                "Also check the <a href='https://eta.epk.ericsson.se/forums/viewforum.php?f=29'>forum "
        }
        jobDescription +=  "for blinking test cases</a> and other info. " +
                "<br>\n" +
                "<h3>I need more logs, where can I find it?</h3>\n" +
                "Tasks are executed with tapas, find your session url in the logs " +
                "<br>\n" +
                "<h3>It's release day and everything is broken?</h3>\n" +
                "Latest rpm with latest versions must be installed on the host, try retrigger " +
                "this job after a new rpm have been created and automatically installed." +
                "<br>\n" +
                "<h3>Multi repo commits?</h3>\n" +
                "No, does not support this. Your best option is to structure into atomic commits " +
                "that all work separately." +
                "<br>\n" +
                "<br>\n" +
                "<h3>Builds are failing due to clusters being marked as dirty?</h3>\n" +
                "Check <a href=\"https://eta.epk.ericsson.se/jenkins/job/" +
                projectName + "_gerrit_jive_cluster_upgrade\">\n" +
                "this</a> job, which performs clusters reinstallation.  <br>\n"  +
                "If there are any builds ongoing, wait until the reinstallation \n" +
                "process will finish and try to build your job again." +
                "<br>\n"

        String continuousJobName = projectName.equals("rmca") ? 'rmca_continuous_targethost_install' :
                'charging_continuous_targethost_install'
        String continuousJobUrl = projectName.equals("rmca") ?
                'https://eta.epk.ericsson.se/jenkins/job/rmca_continuous_targethost_install/' :
                'https://internal.eta.epk.ericsson.se/jenkins/job/charging_continuous_targethost_install/'

        jobDescription += "<h3>I need more info on how this work!</h3>\n" +
                "There is one slower background job that runs continuously, the " +
                "'" + continuousJobName + "'\n" +
                "'" + continuousJobUrl + "'\n" +
                "This job reinstall continuously in a loop oldest cluster with OVF from latest " +
                "green/yellow washingmachine and latest RPM from ARM." +
                "<br>" +
                "And lastly the *_gerrit_jive_test job will " +
                "take latest installed cluster and apply the patches and then run jive tests."

        return jobDescription
    }
}
