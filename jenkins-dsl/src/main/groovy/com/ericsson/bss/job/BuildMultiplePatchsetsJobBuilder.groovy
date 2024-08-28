package com.ericsson.bss.job

import com.ericsson.bss.AbstractGerritJobBuilder

import javaposse.jobdsl.dsl.Job

public class BuildMultiplePatchsetsJobBuilder extends AbstractGerritJobBuilder {

    // must be sufficient to build integration tests
    protected static final int TIMEOUT_PER_PATCH_SET = 30

    protected static final String JOB_DESCRIPTION = "<h2>This job builds a number of patchset's.</h2>\n" +
    "<p>It can be used to test \"multi repository commits\" in gerrit. It will fetch and build the latest patchset for a review.</p>\n" +
    "<p>The <b>build order</b> will be according to the order in the input list.</p>\n" +
    "<h3>Use-Case</h3>\n" +
    "<p>In this example there are two gerrit changes in two different repositories that are dependent on each other.</p>\n" +
    "<pre>\n" +
    ",--------------.      ,-------------------.\n" +
    "| Component A' |<---->| Integration Test' |\n" +
    "| (gerrit)     |      | (gerrit)          |\n" +
    "`--------------'      `-------------------'\n" +
    "\n" +
    ",--------------.      ,-------------------.\n" +
    "| Component A  |<---->| Integration Test  |\n" +
    "`--------------'      `-------------------'\n" +
    "</pre>\n" +
    "<ul>\n" +
    "<li>Component A and Integration Test (IT) are located in different repositories.</li>\n" +
    "<li>Component A has an interface that is used by a test in the Integration Test.</li>\n" +
    "</ul>\n" +
    "<p>If a non backward compatible change is needed in the interface it may lead to a failing testcase in the IT.<br>\n" +
    "In the current build environment we can't build patchsets from different repositories.<br/>\n" +
    "Until one of the patchsets is submitted and breaking the build(s) we can't run a build that is dependent on the other patchset.</p>\n" +
    "<p>This job tries to solve this issue.<br/>\n" +
    "It fetches the patchsets for Component A and IT from the input list, provided by the user, and builds them together according to the order, " +
    "in which they were inputted.</p>\n" +
    "<h3>Known issues</h3>\n" +
    "<ul>\n" +
    "  <li>There won't be any feedback given to the certain reviews.<br/>\n" +
    "It will either be Successful or Failing, it's then up to the user to decide if the patchset(s) should be submitted or not.</li>\n" +
    "  <li>This job can only build commits that are known to gerrit. Means the patchset is/has been a gerrit review.</li>\n" +
    "  <li>Currently, the default timeout for each GERRIT_PATCHSET_ID is set to " +
    TIMEOUT_PER_PATCH_SET + " minutes. It means, that the total timeout for a build \n" +
    "is equal to " + TIMEOUT_PER_PATCH_SET + "*number of provided " +
    "GERRIT_PATCHSET_IDS. <br> If the default value is not sufficient and needed " +
    "to be increased, please contact \n" +
    "<a href=\"https://eta.epk.ericsson.se/helpdesk\">ETA Helpdesk</a> " +
    "for assistance.</li>\n" +
    "</ul>"

    protected String projectName

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))

        addBuildMultiplePatchsetConfig()

        return job
    }

    private void addBuildMultiplePatchsetConfig() {
        job.with {
            description(JOB_DESCRIPTION)

            parameters {
                stringParam('GERRIT_PATCHSET_IDS', null, 'Gerrit Patchset-Id, Change-Id, commit hash etc, seperate id\'s by comma. ' +
                        'Example 266558,0b50a05ae665b47e49acf21cfb6d7e899db29468')
            }

            injectEnv(getInjectVariables())

            steps {
                shell(getShellCommentDescription("Initialize workspace") + createHomeTmpFolder())
                shell(buildMultiplePatchsetsCommand())
            }
        }
    }

    private String buildMultiplePatchsetsCommand() {
        String command = '#!/bin/bash \n' +
                'IFS=\',\' read -r -a array <<< "${GERRIT_PATCHSET_IDS}" \n' +
                'for GERRIT_PATCHSET_ID in "${array[@]}" ; do \n' +
                '\ncd \${WORKSPACE}\n' +
                '\n' + getGerritProjectNameCommand() + '\n' +
                '\n' + getGerritReferenceCommand() + '\n' +
                '\n' + getGerritRevisionCommand() + '\n' +
                '\n' + gitInitiateRepository('\${GERRIT_PATCHSET_ID}') + '\n' +
                '\n' + getGitCache('${GERRIT_PATCHSET_ID}') + '\n' +
                '\n' + gitFetchGerritChange('\${GERRIT_PATCHSET_ID}') + '\n' +
                '\n' + mavenBuildCommand() + '\n' +
                '\n done \n'

        return command
    }

    private String getGerritProjectNameCommand() {
        return getShellCommentDescription('Get gerrit projectname') + 'GERRIT_PROJECT=$(ssh -p 29418 ' + gerritUser + '@' + gerritServer +
                ' gerrit query --current-patch-set ${GERRIT_PATCHSET_ID} | grep "project:" | awk -F \' \' \'{print $2}\')'
    }

    private String getGerritReferenceCommand() {
        return getShellCommentDescription('Get the gerrit references') + 'GERRIT_REFSPEC=$(ssh -p 29418 ' + gerritUser + '@' + gerritServer +
                ' gerrit query --current-patch-set ${GERRIT_PATCHSET_ID} | grep "ref:" | awk -F \' \' \'{print $2}\')'
    }

    private String getGerritRevisionCommand() {
        return getShellCommentDescription('Get the gerrit revision') + 'GERRIT_PATCHSET_REVISION=$(ssh -p 29418 ' + gerritUser + '@' + gerritServer +
                ' gerrit query --current-patch-set ${GERRIT_PATCHSET_ID} | grep "revision:" | awk -F \' \' \'{print $2}\')'
    }

    private String mavenBuildCommand() {
        return  getShellCommentDescription("Maven build command") +
                "mvn \\\n" +
                "clean install \\\n" +
                "-Dsurefire.useFile=false \\\n" +
                getMavenGeneralBuildParameters()
    }

    @Override
    protected String getScriptToSetDynamicTimeout() {
        String dynamicTimeoutScript = dslFactory.readFileFromWorkspace("scripts/setDynamicTimeoutIfRelease.groovy")

        // replace the normal 'defaultTimeout' variable definition with
        // the evaluation, based on the number of the provided patch set ids
        return dynamicTimeoutScript.replaceAll(
                "([ \t]+)//Replace %DEFAULT_TIMEOUT% by current timeout\n\\s+int defaultTimeout = %DEFAULT_TIMEOUT%",
                "\$1String ids = build.buildVariableResolver.resolve(\"GERRIT_PATCHSET_IDS\")\n" +
                "\$1// number of jobs * suitable timeout for integration tests\n" +
                "\$1int defaultTimeout = ids.split(\"[, ]+\").size() * " +
                TIMEOUT_PER_PATCH_SET)
    }
}
