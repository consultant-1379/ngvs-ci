package com.ericsson.bss.job

import com.ericsson.bss.AbstractJobBuilder

import javaposse.jobdsl.dsl.Job

import com.ericsson.bss.util.GitUtil

public class SiteJobBuilder extends AbstractJobBuilder {

    public static final String JOB_DESCRIPTION = "<h2>Build and publish the documentation related for the repository.</h2>\n" +
    DETAILED_JOB_CONFIGURATION_DESCRIPTION +
    "<p>By running <code>mvn site</code> a project report in html will be generated.<br />\n" +
    "Maven site documentation can be written in different <a href=\"https://maven.apache.org/doxia/references/\">languages</a>.</p>" +
    "<h4>Some parts that are generated</h4>\n" +
    "<ul>\n" +
    "  <li>JavaDoc</li>\n" +
    "  <li>Dependency usage</li>\n" +
    "  <li>.. and much more</li>\n" +
    "</ul>\n" +
    "<p>The site will be deployed on <a href='https://eta.epk.ericsson.se/maven-sites/latest/'>" +
    "https://eta.epk.ericsson.se/maven-sites/latest/</a></p>\n" +
    BSSF_MAVEN_CI_DESCRIPTION

    protected String extraMavenOptions = ""

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        addSiteConfig()
        return job
    }

    protected void addSiteConfig() {
        job.with {
            description(DSL_DESCRIPTION + JOB_DESCRIPTION)

            addGitRepository(gerritName)
            customWorkspace("workspace/\${JOB_NAME}")
            addTimeoutConfig()
            triggers {
                if (GitUtil.isLocatedInGitolite(gerritName)) {
                    scm(SCM_POLLING_FREQUENT)
                } else {
                    scm(SCM_POLLING + '\n# Realtime pushed by the eta_gitscmpoll_trigger job')
                }
            }
            steps {
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
                shell(removeOldArtifacts())
                shell(gitConfig("\${WORKSPACE}"))
                if (generateGUIconfig) {
                    shell(gconfWorkspaceWorkaround())
                }

                String buildSiteCommand = "package site" + extraMavenOptions + " site:deploy"
                shell(deploySiteCommand("clean " + buildSiteCommand))
                shell(deploySiteCommand(buildSiteCommand + " \\\n" + "-Dmaven.site.filePath=\\\${user.home}/maven-sites/latest/\\\${project.groupId}"))
            }
            injectEnv(getInjectSiteVariables())
            addExtendableEmail()

            publishers { wsCleanup() }
        }
    }

    protected String deploySiteCommand(String mavenSiteComm) {
        String cmd = getShellCommentDescription("Deploy maven site version and latest") +
                "mvn \\\n" +
                mavenSiteComm + " \\\n"

        if (profilesToBeUsed != null && profilesToBeUsed != "") {
            cmd += '-P' + profilesToBeUsed + ' \\\n'
            cmd += '-Dskip.cdt2.build=true \\\n'
        }
        cmd += '-DskipTests \\\n'
        cmd += getMavenGeneralBuildParameters()

        return cmd
    }

    protected Map getInjectSiteVariables() {
        Map env_list = getInjectVariables()

        env_list.put("GRAPHVIZ_DOT", GRAPHVIZ_HOME + "/dot")
        env_list['PATH'] += ":" + GRAPHVIZ_HOME
        return env_list
    }
}
