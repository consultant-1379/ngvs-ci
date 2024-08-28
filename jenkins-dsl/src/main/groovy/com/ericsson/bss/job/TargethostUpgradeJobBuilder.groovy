package com.ericsson.bss.job

import com.ericsson.bss.AbstractTapasJobBuilder

import javaposse.jobdsl.dsl.Job

public class TargethostUpgradeJobBuilder extends AbstractTapasJobBuilder {

    protected boolean useCil = true
    protected String versionLocation = ""
    protected int nrOfNetworks = 1

    public Job build() {
        super.buildDescription = "\$TARGETHOST \$CURRENTVERSION \$NEWVERSION"
        suiteFile = suite.split ('/')[-1].replace('.xml', '_\${TARGETHOST}.xml')
        initProject(dslFactory.freeStyleJob(jobName))
        setDescription("<br>\n" +
                       "This job is used to test upgrade " + projectName + " on a cluster.<br><br>Note: The job will remove TARGETHOST, install " +
                       "and then upgrade, since the upgrade scripts won't allow upgrade on a host with snapshots.<br>\n")

        addTimeoutAndAbortConfig(timeoutForJob)
        deleteWorkspaceBeforeBuildStarts()

        super.configurePostBuildSteps().editableEmailNotification(getAlwaysMailConfig())

        Map env_list = getInjectVariables()
        env_list.remove("HOME")
        injectEnv(env_list)
        job.with {
            steps {
                environmentVariables {
                    propertiesFile('env.properties')
                }
            }
            publishers {
                wsCleanup()
            }
        }
        return job
    }

    @Override
    protected void setInputParameters() {
        addSelectClusterParameter('Choose cluster to upgrade')

        addClusterReferenceParameter('MSV', 'The MSV for this cluster')
        if (useCil) {
            addClusterReferenceParameter('CIL', 'The CIL for this cluster')
        }
        addClusterReferenceParameter('TARGETHOST', 'The host which will be deleted, installed and upgraded')

        addVersionChoiceParam('CURRENTVERSION', versionLocation,
                              'Version of ' + projectName.toUpperCase() + ' to install and test upgrade from.', false, false)
        addVersionChoiceParam('NEWVERSIONS_AVAILABLE', versionLocation,
                              'Version of ' + projectName.toUpperCase() + ' to upgrade to.', true, false)
        addReferenceParameter('NEWVERSION', 'NEWVERSIONS_AVAILABLE', 'Select a version from above or if using DVFILE enter the appropriate version matching ' +
                                                                     'the supplied DVFILE.')
        job.with {
            parameters {
                stringParam('DVFILE', '',
                            'Optional. A custom file to use instead of an artifact from ARM (' + projectName + '-*-dv.tar.gz). ' +
                            'The file must be accessible from Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL. ' +
                            'Leave empty to download the specified NEWVERSION from ARM.')
                stringParam('VMAPI_PREFIX', projectName + '.',
                            'To use correct credentitials and vCenter. Normally the product the host belongs to. Ex: "cil.", "charging.", "cpm.".')
                stringParam('PRODUCT_CONFIG_DIR', '/proj/eta-automation/tapas/config/' + projectName + '/',
                        'Optional. Only change this if you have a custom product config dir for suite or other config files (except hosts).')
                if (nrOfNetworks > 1) {
                    choiceParam('NUM_OF_NETWORK', getNumberedList(1, nrOfNetworks),
                                'Number of network interfaces (vNIC) to deploy. Default is only one network interface (vNIC). It will take network information ' +
                                        'available in IPDB')
                }
            }
        }
        setExtraOptions()
    }

    protected void setExtraOptions() {
        return
    }

    @Override
    protected String getTapasParameters() {
        String params = '--define=__MSV__=${MSV} \\\n'
        if (useCil) {
            params += '--define=__CIL__=\${CIL} \\\n'
        }
        params += '--define=__TARGETHOST__=${TARGETHOST} \\\n' +
                  '--define=__CURRENTVERSION__=${CURRENTVERSION} \\\n' +
                  '--define=__NEWVERSION__=${NEWVERSION} \\\n' +
                  '--define=__DVFILE__=\${DVFILE} \\\n' +
                  '--define=__VMAPI_PROFILE_PREFIX__=${VMAPI_PREFIX} \\\n' +
                  '--define=__HOST_PROFILE_PREFIX__=${HOST_PREFIX} \\\n' +
                  '--define=__PRODUCT_CONFIG_DIR__=${PRODUCT_CONFIG_DIR}/config/ \\\n'

        if (nrOfNetworks > 1) {
            params +=   '--define=__NUM_OF_NETWORK__="${NUM_OF_NETWORK}" \\\n'
        }

        return params
    }

    @Override
    protected String getAdditionalFinishingTapasShell() {
        return '\n' +
               'SUCCESS="SUCCESS"\n' +
               'echo "SUCCESS=\$SUCCESS" >> env.properties\n' +
               'echo \$SUCCESS\n'
    }
    @Override
    protected String getTapasConfigSettings() {
        String config = 'BASE_CONFIG_FILE="${PRODUCT_CONFIG_DIR}/' + suite + '"\n' +
                        'CONFIG_FILE="/proj/eta-automation/tapas/sessions/' + tapasProjectDestinationName + '/' + suiteFile + '"\n'
        return config
    }
}
