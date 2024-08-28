package com.ericsson.bss.job.cpm

import com.ericsson.bss.job.SiteJobBuilder
import com.ericsson.bss.util.GitUtil

public class CpmSiteJobBuilder extends SiteJobBuilder{
    private String cpmMavenSiteComm = "site-deploy"

    @Override
    protected void addSiteConfig(){
        job.with {

            String jobDescription = "" +
                    "<h2>This job creates the maven site.</h2>" +
                    "<p>The site will be deployed on <a href='https://eta.epk.ericsson.se/maven-sites/latest/'>" +
                    "https://eta.epk.ericsson.se/maven-sites/latest/</a></p>"

            description(DSL_DESCRIPTION + jobDescription)

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
                shell(cleanUpWorkspace("\${WORKSPACE}"))
                shell(removeOldArtifacts())
                shell(gitConfig("\${WORKSPACE}"))
                if (generateGUIconfig) {
                    shell(gconfWorkspaceWorkaround())
                }
                shell(deploySiteCommand(cpmMavenSiteComm))
            }
            injectEnv(getInjectSiteVariables())
            addExtendableEmail()
        }
    }
}
