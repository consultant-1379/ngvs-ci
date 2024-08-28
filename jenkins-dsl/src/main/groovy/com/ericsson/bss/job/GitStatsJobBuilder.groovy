package com.ericsson.bss.job

import javaposse.jobdsl.dsl.Job

import com.ericsson.bss.util.GitUtil
import com.ericsson.bss.AbstractJobBuilder

class GitStatsJobBuilder extends AbstractJobBuilder {

    private List repositories

    private String projectName

    private static final String JOB_DESCRIPTION = "<h2>Job that generate statistics for git repositories</h2>\n" +
            "<p>Possible to see number of commits per month, year, most productive day and time.<br/>\n" +
            "New statistics is generated a few times per day.</p>\n" +
            "<p>Please see last <a href=\"lastSuccessfulBuild/artifact/gitstats_out/index.html\">published</a> statistics.</p>"

    public Job build() {
        timeoutForJob = 45
        initProject(dslFactory.freeStyleJob(jobName))
        addGitStatsConfig()
        return job
    }

    private void addGitStatsConfig() {
        job.with {

            description(JOB_DESCRIPTION)

            logRotator(DAYS_KEEP, BUILDS_KEEP, DAYS_KEEP, 1)

            customWorkspace(CUSTOM_WORKSPACE_MESOS)

            addTimeoutConfig()

            def envList = getInjectVariables()
            String path = envList.getAt("PATH")
            envList.put('PATH', '/opt/local/dev_tools/gnuplot/latest/bin/:'+path)
            envList.put('PROJECT_NAME', projectName)

            wrappers {
                environmentVariables { envs(envList) }
            }

            triggers { cron('H 6,12,18 * * *') }

            steps {
                for (repository in repositories) {
                    shell(getCloneOrFetch(repository))
                }

                shell(getShellCommentDescription('Generate git statistics') + dslFactory.readFileFromWorkspace('scripts/gitstats.sh'))
            }

            publishers {
                archiveArtifacts {
                    pattern('gitstats_out/**')
                    onlyIfSuccessful(true)
                }

                wsCleanup()
            }
        }
    }

    private String getCloneOrFetch(String repositoryName) {
        String gitURL = GitUtil.getGitUrl(gerritServer, repositoryName)
        String repositoryFolder = "\${WORKSPACE}/" + repositoryName
        String gitCloneFolder = formatRepositoryName(repositoryName)

        return "" +
                getShellCommentDescription("Repository for " + repositoryName) +
                "if [ -d \""+repositoryFolder+"\" ]; then\n" +
                "  cd " + repositoryFolder + "\n" +
                "  /opt/local/dev_tools/git/latest/bin/git fetch " + gitURL + " master:master\n" +
                "else\n" +
                "  /opt/local/dev_tools/git/latest/bin/git " +
                "clone --reference " + GitUtil.getCloneReference() + " --bare " + gitURL + " " + gitCloneFolder + "\n" +
                "fi"
    }

    private String formatRepositoryName(String reposiotryName) {
        assert reposiotryName != null

        if (reposiotryName.startsWith("kagitolite") || reposiotryName.startsWith("ssh://kagitolite")) {
            reposiotryName = reposiotryName.replace("ssh://", "").replace("kagitolite", "")
        }

        if (reposiotryName.length() > 0 && reposiotryName.startsWith("/") || reposiotryName.startsWith(":")) {
            reposiotryName = reposiotryName[1..-1]
        }
        reposiotryName
    }
}
