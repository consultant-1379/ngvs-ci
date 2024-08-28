package com.ericsson.bss.job.cil

import com.ericsson.bss.job.DeployJobBuilder

class CilCreateSoftwareRecordDeploy extends DeployJobBuilder {
    protected boolean generateSoftwareRecord = false
    protected getDeployConfig() {
        timeoutForJob = 30
        if (gerritName.contains("server")) {
            Closure preSteps = {
                shell(cleanUpWorkspaceMesos("\${WORKSPACE}"))
                shell(removeOldArtifacts())
                shell(gitConfig("\${WORKSPACE}"))
                if (generateGUIconfig) {
                    shell(gconfWorkspaceWorkaround())
                }
            }

            Map environmentVariables = getInjectVariables()

            job.with {
                preBuildSteps(preSteps)

                injectEnv(environmentVariables)

                addMavenConfig()
                if (workspacePath != null && workspacePath != "") {
                    customWorkspace(workspacePath)
                }
                addMavenRelease()
                addTimeoutConfig()
            }

        }
        else {
            super.getDeployConfig()
        }
        if (generateSoftwareRecord) {
            createSoftwareRecord()
        }
    }

    private boolean getIncludeSources() {
        return !gerritName.contains("server")
    }

    private String getArtifactAddress() {
        return "/proj-cil-release-local/com/ericsson/bss/cil/" + getRepositoryName() + "/"
    }

    private String getArtifactId() {
        if (gerritName.contains("server")) {
            return "cil-server-dv"
        }
        else if (gerritName.contains("service")) {
            return "service"
        }
        else if (gerritName.contains("tools")) {
            return "ciltool"
        }
        return "client"
    }

    private String getRepositoryName() {
        if (gerritName.contains("messaging")) {
            return "messaging"
        }
        else if (gerritName.contains("client")) {
            return "client"
        }
        String[] repositoryName = gerritName.split("_")
        return repositoryName.last()
    }

    protected void createSoftwareRecord() {
        job.with {
            postBuildSteps('SUCCESS') {
                conditionalSteps {
                    condition {
                        and { booleanCondition('\${IS_M2RELEASEBUILD}') }
                                { expression('false', '\${MVN_ISDRYRUN}') }
                    }
                    runner('DontRun')
                    steps {
                        downstreamParameterized {
                            trigger('cil_software_record') {
                                parameters {
                                    predefinedProps([
                                        PRODUCTNAME: projectName,
                                        PRODUCTREPOSITORY: gerritName,
                                        ARTIFACTADDRESS: getArtifactAddress(),
                                        ARTIFACTID: getArtifactId(),
                                        GROUPID: 'com.ericsson.bss.cil.' + getRepositoryName(),
                                        INCLUDESOURCES: getIncludeSources(),
                                        REMOTETARGETDIR: 'proj-cil-release-local',
                                        VERSION: '\${MVN_RELEASE_VERSION}'
                                    ])
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
