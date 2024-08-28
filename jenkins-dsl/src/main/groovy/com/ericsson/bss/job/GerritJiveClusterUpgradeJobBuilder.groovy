package com.ericsson.bss.job

import com.ericsson.bss.AbstractJobBuilder

import javaposse.jobdsl.dsl.Job

public class GerritJiveClusterUpgradeJobBuilder extends AbstractJobBuilder {

    protected String tpgName = ""
    protected String msvProfile = ""
    protected String cilProfile = ""
    protected String targethostProfile = ""

    public Job build() {
        if (tpgName == "") {
            tpgName = this.projectName.split("\\.")[0]
        }
        initProject(dslFactory.freeStyleJob(jobName))
        addConfig()
        addBuildSteps()
        addPublishers()
        return job
    }

    protected void addConfig() {
        job.with {
            description(DSL_DESCRIPTION +
                "Job that continiously upgrades MSV, CIL (if needed) and TPG. During work hours \n" +
                "only dirty clusters will be touched and during night all will be updated.")

        }
        setThrottleConcurrentConfig(10)
        buildPeriodically("H/10 * * * *")
        addTimeoutAndAbortConfig(360)
        setEnvironmentVariables()
        deleteWorkspaceBeforeBuildStarts()
    }

    protected String getProductFolder() {
        return tpgName
    }

    protected void addBuildSteps() {
        job.with {
            steps {
                shell(getClusterShell())
                conditionalSteps {
                    condition {
                        shell('#!/bin/bash\necho "Find out if we have a selected cluster"\n' +
                              '/bin/grep CLUSTER_USED env.properties\nexit \$?')
                    }
                    steps {
                        downstreamParameterized {
                            trigger('compare_and_upgrade_msv_cil_versions') {
                                block {
                                    buildStepFailure('FAILURE')
                                    failure('FAILURE')
                                    unstable('UNSTABLE')
                                }
                                parameters {
                                    predefinedProps([VMAPI_PREFIX: tpgName + '.',
                                                     MSVCIL_SETTINGS: PATH_TO_JOB_CONFIG + tpgName +
                                                                      '_washingmachine_params.properties',
                                                     PRODUCT: tpgName,
                                                     INSTALLATIONTYPE: 'msv_cil',
                                                     MSV_RESOURCE_PROFILE: msvProfile,
                                                     CIL_RESOURCE_PROFILE: cilProfile])
                                    propertiesFile("env.properties", true)
                                }
                            }
                        }
                        downstreamParameterized {
                            trigger(tpgName + '_targethost_install') {
                                block {
                                    buildStepFailure('FAILURE')
                                    failure('FAILURE')
                                    unstable('UNSTABLE')
                                }
                                parameters {
                                    predefinedProps([INSTALLTYPE: 'full',
                                                     OPEN_PORTS: 'true',
                                                     RESOURCE_PROFILE: targethostProfile,
                                                     VERSION: 'LATEST'])
                                    propertiesFile("env.properties", true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected void addPublishers() {
        job.with {
            publishers {
                flexiblePublish {
                    conditionalAction {
                        condition {
                            alwaysRun()
                        }
                        steps {
                            shell("#!/bin/bash -x\n" +
                                  "echo \"Remove cluster upgrade file\"\n" +
                                  "source env.properties\n" +
                                  "if [[ -n \$CLUSTER_UPGRADE_FILE ]]; then\n" +
                                  "    rm -f \$CLUSTER_UPGRADE_FILE\n" +
                                  "else\n" +
                                  "    echo \"No upgrade was started, no file to remove\"\n" +
                                  "fi")
                        }
                    }
                }
                flexiblePublish {
                    conditionalAction {
                        condition {
                            status("SUCCESS", "SUCCESS")
                        }
                        steps {
                            shell("#!/bin/bash -x\n" +
                                  "echo \"Remove cluster dirty file\"\n" +
                                  "source env.properties\n" +
                                  "if [[ -n \$CLUSTER_DIRTY_FILE ]]; then\n" +
                                  "    rm -f \$CLUSTER_DIRTY_FILE\n" +
                                  "else\n" +
                                  "    echo \"No dirty file was found, no file to remove\"\n" +
                                  "fi\n" +
                                  "\n" +
                                  "if [[ -n \$CLUSTER_FILE_USED ]]; then\n" +
                                  "    touch \"\$CLUSTER_FILE_USED\"\n" +
                                  "else\n" +
                                  "    echo \"No cluster file was used, no file to touch\"\n" +
                                  "fi")
                        }
                    }
                }
            }
        }
    }

    private void setEnvironmentVariables() {
        Map variables = getInjectVariables()
        variables.put("PRODUCT_FOLDER", getProductFolder())
        variables.put("#", "")
        variables.put("#Set true to force this job to reinstall the MSV and/or CIL", "")
        variables.put("#on all clusters currently in the list", "")
        variables.put("FORCE_MSV_CIL_INSTALLATION", "false")

        injectPortAllocation = ""
        injectEnv(variables)
    }

    protected String getClusterShell() {
        return "#!/bin/bash\n" +
               "cd \${WORKSPACE}\n" +
               "\n" +
               "readonly LOCKFILE_DIR=/proj/eta-automation/jenkins/kascmadm/clusters/" +
                                     "\$PRODUCT_FOLDER/locks\n" +
               "readonly CLUSTER_CONFIG_DIR=/proj/eta-automation/jenkins/kascmadm/clusters/" +
                                     "\$PRODUCT_FOLDER/config\n" +
               "readonly CLUSTER_STATUS_DIR=/proj/eta-automation/jenkins/kascmadm/clusters/" +
                                     "\$PRODUCT_FOLDER/status\n" +
               "CLUSTER_DIR=\"\${CLUSTER_CONFIG_DIR}/*.json\"\n" +
               "\n" +
               "# Set SKIP_DIRTY_CLUSTER=true to skip dirty clusters.\n" +
               "# Use SKIP on regtests. Do not set SKIP in cluster wipe/reinstall jobs\n" +
               "\n" +
               "main() {\n" +
               "    minutes_to_retry=1\n" +
               "    RETRY_IN_SECONDS=\$((60 * \$minutes_to_retry))\n" +
               "    # Retry locking a cluster for \$RETRY_IN_SECONDS seconds\n" +
               "    for clusterfile in `ls -tr \${CLUSTER_DIR}`\n" +
               "    do\n" +
               "        echo \"Trying to lock cluster \$clusterfile\"\n" +
               "        cluster=\$(basename \${clusterfile})\n" +
               "        cluster=\"\${cluster%.*}\"\n" +
               "        export CLUSTER_DIRTY_FILE=\${CLUSTER_STATUS_DIR}/\${cluster}.dirty\n" +
               "        export CLUSTER_UPGRADE_FILE=\${CLUSTER_STATUS_DIR}/\${cluster}.upgrade\n" +
               "        if [ -f \${CLUSTER_DIRTY_FILE} -a \"\${SKIP_DIRTY_CLUSTER:-false}\" == \"true\" ]; then\n" +
               "            echo \"Cluster \${cluster} is marked as dirty, skipping\"\n" +
               "            echo \"\"\n" +
               "            continue\n" +
               "        elif [ -f \$CLUSTER_UPGRADE_FILE ]; then\n" +
               "            echo \"Cluster \${cluster} is marked as being upgraded, skipping\"\n" +
               "            echo \"\"\n" +
               "            continue\n" +
               "        fi\n" +
               "        hour=`date +%H`\n" +
               "        if [ -f \${CLUSTER_DIRTY_FILE} ] || [ \$hour -gt 17 -o \$hour -lt 4 ]; then\n" +
               "            lock \"\$cluster\" || continue;\n" +
               "            export CLUSTER_USED=\$cluster\n" +
               "            export CLUSTER_FILE_USED=\$clusterfile\n" +
               "            get_cluster_hosts\n" +
               "            echo \"CLUSTER_USED=\$CLUSTER_USED\" >> env.properties\n" +
               "            echo \"CLUSTER_FILE_USED=\$CLUSTER_FILE_USED\" >> env.properties\n" +
               "            echo \"CLUSTER_UPGRADE_FILE=\$CLUSTER_UPGRADE_FILE\" >> env.properties\n" +
               "            echo \"CLUSTER_DIRTY_FILE=\$CLUSTER_DIRTY_FILE\" >> env.properties\n" +
               "            echo \"FORCEINSTALLATION=\$FORCE_MSV_CIL_INSTALLATION\" >> env.properties\n" +
               "            date \"+%F %T \${BUILD_TAG}\" >> \${CLUSTER_DIRTY_FILE}\n" +
               "            exit 0;\n" +
               "        else\n" +
               "            echo \"Cluster \${cluster} is not dirty and it is work hours, skipping\"\n" +
               "            echo \"\"\n" +
               "            continue\n" +
               "        fi\n" +
               "    done\n" +
               "    echo \"Could not find a available cluster for installation\"\n" +
               "    exit 0\n" +
               "}\n" +
               "\n" +
               "lock() {\n" +
               "    lock_fd=200\n" +
               "    cluster=\$1\n" +
               "    lock_file=\${LOCKFILE_DIR}/\${cluster}.lock\n" +
               "\n" +
               "    # Create lock file\n" +
               "    eval \"exec \${lock_fd}>>\${lock_file}\"\n" +
               "\n" +
               "    # Acquire the lock\n" +
               "    flock -n \${lock_fd}\n" +
               "    result=\$?\n" +
               "    if [ \${result} -eq 0 ]; then\n" +
               "        date \"+%F %T \${BUILD_TAG}\" >> \${CLUSTER_UPGRADE_FILE}\n" +
               "        echo \"Acquired lock on \${cluster}\"\n" +
               "    else\n" +
               "        echo \"Could not acquire lock on \${cluster}\"\n" +
               "        cat \${lock_file}\n" +
               "    fi\n" +
               "\n" +
               "    echo \"\"\n" +
               "    return \${result}\n" +
               "}\n" +
               "\n" +
               "get_cluster_hosts() {\n" +
               "    echo \"get hosts: \${CLUSTER_USED}\"\n" +
               "\n" +
               "    if [ ! -f \"\$CLUSTER_FILE_USED\" ]; then\n" +
               "        echo \"File not found: \$CLUSTER_FILE_USED\"\n" +
               "        exit 1\n" +
               "    fi\n" +
               "\n" +
               "    MSV=`/bin/cat \${CLUSTER_FILE_USED} | python -c 'import json,sys;" +
                                                                     "obj=json.load(sys.stdin);" +
                                                                     "print obj[\"'\${CLUSTER_USED}'\"]" +
                                                                     "[\"msv\"]'`\n" +
               "    echo \"MSV=\${MSV}\" >> env.properties\n" +
               "    echo \"MSV: \$MSV\"\n" +
               "\n" +
               "    CIL=`/bin/cat \${CLUSTER_FILE_USED} | python -c 'import json,sys;" +
                                                                     "obj=json.load(sys.stdin);" +
                                                                     "print obj[\"'\${CLUSTER_USED}'\"]" +
                                                                     "[\"cil\"]'`\n" +
               "    echo \"CIL: \$CIL\"\n" +
               "    echo \"CIL=\${CIL}\" >> env.properties\n" +
               "\n" +
               getTpgSpecificParameters() +
               "}\n" +
               "main\n"
    }

    protected String getTpgSpecificParameters() {
        return "    TARGETHOST=`/bin/cat \${CLUSTER_FILE_USED} | python -c 'import json,sys;" +
                                                                           "obj=json.load(sys.stdin);" +
                                                                           "print obj[\"'\${CLUSTER_USED}'\"]" +
                                                                           "[\"targethost\"]'`\n" +
               "    echo \"TARGETHOST=\${TARGETHOST}\" >> env.properties\n" +
               "    echo \"TARGETHOST: \$TARGETHOST\"\n"

    }
}
