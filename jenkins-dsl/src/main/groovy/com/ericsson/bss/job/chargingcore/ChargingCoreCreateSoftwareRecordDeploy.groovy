package com.ericsson.bss.job.chargingcore

import com.ericsson.bss.job.DeployJobBuilder

class ChargingCoreCreateSoftwareRecordDeploy extends DeployJobBuilder {
    protected boolean generateSoftwareRecord = false
    protected getDeployConfig() {
        super.getDeployConfig()
        if (generateSoftwareRecord) {
            createSoftwareRecord()
        }
    }

    protected void createSoftwareRecord() {
        String [] productRepository = gerritName.split('/')
        job.with {
            postBuildSteps {
                conditionalSteps {
                    condition {
                        and { booleanCondition('\${IS_M2RELEASEBUILD}') }
                                { expression('false', '\${MVN_ISDRYRUN}') }
                    }
                    runner('DontRun')
                    steps {
                        downstreamParameterized {
                            trigger('charging.core_software_record') {
                                parameters {
                                    predefinedProps([
                                        PRODUCTNAME: projectName,
                                        PRODUCTREPOSITORY: productRepository[1],
                                        ARTIFACTID: projectName,
                                        REMOTETARGETDIR: 'proj-charging-release-local',
                                        ARTIFACTADDRESS: '/proj-charging-release-local/com/ericsson/bss/rm/',
                                        GROUPID: 'com.ericsson.bss.rm',
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
