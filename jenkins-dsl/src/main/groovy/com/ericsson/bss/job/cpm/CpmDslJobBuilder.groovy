package com.ericsson.bss.job.cpm

import com.ericsson.bss.job.DslJobBuilder

public class CpmDslJobBuilder extends DslJobBuilder{

    @Override
    protected void addPermissionConfig(){
        job.with {
            environemntVariableMap.put('PERMISSION_FILE', "jenkins-cpm_perms.json")

            publishers {
                flexiblePublish {
                    conditionalAction {
                        condition { alwaysRun() }
                        steps { shell(dslFactory.readFileFromWorkspace('scripts/set_job_permissions.sh')) }
                    }
                }

                mailer('', false, true)
            }
        }
    }
}
