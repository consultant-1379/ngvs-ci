package com.ericsson.bss.job

import com.ericsson.bss.AbstractTapasJobBuilder
import javaposse.jobdsl.dsl.Job

public class OvfBuildJobBuilder extends AbstractTapasJobBuilder {

    protected List<String> variants = []
    protected HashMap<String, String> variantsToOvfPacnames = new HashMap<String, String>()

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        setDescription("<h2>This job is used to build a " + projectName + " OVF.</h2> " +
                        "The OVF will be located on the specified INSTALLNODE at /root/ovf/"
                        + projectName + "/&lt;timestamp&gt;/" + tpgName.toUpperCase() +
                        "-&lt;OVFVERSION&gt;.tar.gz.<br/>NOTE: The OVF is deleted after 3 days.")

        discardOldBuilds(30, 30)
        deleteWorkspaceBeforeBuildStarts()
        setInjectEnv()
        abortBuildIfStuck()
        cleanupWorkspace()

        return job
    }

    @Override
    protected void setInputParameters() {
        job.with {
            parameters {
                stringParam('INSTALLNODE', '', 'The hostname of the machine, which performs the ' +
                                               'installation of the TPG on TARGETHOST.')
                stringParam('TARGETHOST', '', 'The hostname of the machine, where the TPG will ' +
                                              'be installed and from which the OVF will be generated from.')
                if (!variants.empty) {
                    choiceParam('VARIANT', variants, 'Which variant of ' + projectName +
                                                     ' to install and build.')
                }

                stringParam('STAGINGVERSION', 'LATEST', 'Version of the staging directory ' +
                                                        'artifact. Use LATEST to get the lastest ' +
                                                        'SNAPSHOT version.')
                stringParam('OVFVERSION', '', 'Version that the build OVF should have. This is ' +
                                              'part of the filename of the final tar.gz file. If empty "999.9.9", will be used. ')
                choiceParam('UPLOADTOARM', ['no', 'yes'], 'Should the generated OVF be uploaded ' +
                                                          'to ARM.')
                stringParam('VMAPI_PROFILE_PREFIX', tpgName + '.', 'To use correct credentitials ' +
                                                                   'and vCenter. Normally the ' +
                                                                   'product the TARGETHOST ' +
                                                                   'belongs to. Ex: "rmca.", ' +
                                                                   '"cil.", "charging.", "cpm.".')
                stringParam('HOST_PROFILE_PREFIX', '', 'Optional. If the host should be located ' +
                                                       'in a specfic folder in the Resource pool,' +
                                                       ' then add the host prefix here with an ' +
                                                       'ending dot.')
                stringParam('OVF_DESTINATION_FOLDER', '', 'Optional. Location to where the final' +
                                                           ' OVF-tar.gz file should be saved, ' +
                                                           'full path. Ex: "/cpmshare/ovf/test/"')
            }
        }
    }

     protected void setInjectEnv() {
         Map env_list = getInjectVariables()
         env_list.remove("HOME")
         env_list.put("#Fix for mesos setting wrong java ENV ", "")
         env_list.put("PATH", "/proj/env/bin:/usr/bin:/bin")
         env_list.put("_JAVA_OPTIONS", "")
         injectEnv(env_list)
     }

    protected void abortBuildIfStuck() {
        job.with {
            wrappers {
                timeout {
                    absolute(240)
                    abortBuild()
                    writeDescription("Build aborted due to timeout after {0} minutes")
                }
            }
        }
    }

    @Override
    protected String getAdditionalTapasShell() {
        String additionalTapas = '\nBUILDDESC=`echo \${OVFVERSION} \${TARGETHOST}`\n' +
                                 'export BUILDDESC\n'

        additionalTapas += '\n# Set host config dir based on VMAPI prefix\n' +
                           'HOST=`echo ${VMAPI_PROFILE_PREFIX} | sed \'s/\\.*$//\'`\n' +
                           'HOST_CONFIG_DIR="/proj/eta-automation/tapas/config/${HOST}/' +
                           'config/"\n'

        additionalTapas += '\n#Set product config dir\n' +
                           'PRODUCT_CONFIG_DIR="/proj/eta-automation/tapas/config/' + tpgName +
                           '/config/"\n'

        additionalTapas += getOvfPacName()

        additionalTapas += '\nif [[ "${OVFVERSION}" = "" ]]; then\n' +
                '  OVFVERSION="999.9.9"\n' +
                'fi\n'

        return additionalTapas
     }

    protected String getTapasParameters() {
        String params = '--define=__INSTALLNODE__="\${INSTALLNODE}" \\\n'
        params += '--define=__TARGETHOST__="\${TARGETHOST}" \\\n'
        params += '--define=__STAGINGVERSION__="\${STAGINGVERSION}" \\\n'
        params += '--define=__OVFVERSION__="\${OVFVERSION}" \\\n'
        params += '--define=__UPLOADTOARM__="\${UPLOADTOARM}" \\\n'
        params += '--define=__VMAPI_PROFILE_PREFIX__="\${VMAPI_PROFILE_PREFIX}" \\\n'
        params += '--define=__HOST_PROFILE_PREFIX__="\${HOST_PROFILE_PREFIX}" \\\n'
        params += '--define=__HOST_CONFIG_DIR__="\${HOST_CONFIG_DIR}" \\\n'
        params += '--define=__PRODUCT_CONFIG_DIR__="\${PRODUCT_CONFIG_DIR}" \\\n'
        params += '--define=__OVFPACNAME__="\${OVFPACNAME}" \\\n'
        params += '--define=__OVF_DESTINATION_FOLDER__="\${OVF_DESTINATION_FOLDER}" \\\n'
        if (!variants.empty) {
            params += '--define=__VARIANT__="\${VARIANT}" \\\n'
        }
         return params
     }

    protected void cleanupWorkspace(){
        job.with {
            publishers {
                wsCleanup()
            }
        }
    }

    private String getOvfPacName() {
        if (variants.empty || variantsToOvfPacnames.empty) {
            return '\nOVFPACNAME="' + tpgName.toUpperCase() + '"\n'
        }

        String key = variants[0]
        String value = variantsToOvfPacnames.get(key)
        String ovfPacName = ""

        ovfPacName += '\nif [[ "\${VARIANT}" = "' + key + '" ]]; then\n' +
                      '   OVFPACNAME="' + value + '"\n'
        for (String it in variants.subList( 1, variants.size() ) ) {
            value = variantsToOvfPacnames.get(it)
            ovfPacName += 'elif [[ "\${VARIANT}" = "' + it + '" ]]; then\n' +
                          '   OVFPACNAME="' + value + '"\n'
        }

        ovfPacName += 'fi\n'

        return ovfPacName
    }

}
