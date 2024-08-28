package com.ericsson.bss.job.eta

import com.ericsson.bss.job.SonarJobBuilder

class JenkinsDSLSonarJobBuilder extends SonarJobBuilder {

    @Override
    protected void initShellJobs(){
        shells.add(cleanUpWorkspaceMesos("\${WORKSPACE}"))
        shells.add(removeOldArtifacts())
        shells.add(gitConfig("\${WORKSPACE}"))
        if (generateGUIconfig) {
            shells.add(gconfWorkspaceWorkaround())
        }
        if (branchName != "master" && branchName.contains('/')) {
            shells.add(getBranchRenameCommand())
        }
        if (generateGUIconfig) {
            shells.add(getCDTSonarTesReportWorkaround())
        }
    }

    protected void addSonarNature() {
        job.with {
            configure { project ->
                project / builders / 'hudson.plugins.sonar.SonarRunnerBuilder' {
                    installationName('https://sonar.epk.ericsson.se/')
                    properties(getSonarProperties())
                    jdk('(Inherit From Job)')
                    javaOpts(JAVA_TOOL_OPTIONS)
                }
            }
        }
    }

    private static getSonarProperties(){
        return 'sonar.projectKey=com.ericsson.eta.jenkins.dsl\n' +
                'sonar.projectName=ETA Jenkins dsl\n' +
                'sonar.projectVersion=0.1\n' +
                'sonar.sources=src/main/groovy,jobs\n' +
                'sonar.tests=src/test/groovy\n' +
                'sonar.language=grvy\n' +
                'sonar.issuesReport.console.enable=true\n' +
                'sonar.issuesReport.html.enable=true\n' +
                'sonar.analysis.mode=analysis\n' +
                'sonar.profile=ETA\n' +
                'sonar.core.codeCoveragePlugin=jacoco'
    }
}
