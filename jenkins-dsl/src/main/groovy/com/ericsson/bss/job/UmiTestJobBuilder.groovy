package com.ericsson.bss.job

import com.ericsson.bss.AbstractTapasJobBuilder

import javaposse.jobdsl.dsl.Job

public class UmiTestJobBuilder extends AbstractTapasJobBuilder {

    protected boolean useTwoTargethosts = false
    protected boolean withInstallNodePool = false
    protected String installNodePool = ""
    protected String extraDescription = ""
    protected boolean useCil = true

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        super.configurePostBuildSteps().editableEmailNotification(getAlwaysMailConfig())
        setDescription("<h2>Runs installation test for " + projectName + ".</h2>" + extraDescription)
        return job
    }

    @Override
    protected void setInputParameters() {
        addSelectClusterParameter('Choose cluster to update RPM on targethost')
        addClusterReferenceParameter('MSV', 'MSV for this cluster. The MSV used for information ' +
                                     'storage and deploy of the built OVF.')
        if (useCil) {
            addClusterReferenceParameter('CIL', 'CIL for this cluster.')
        }
        addClusterReferenceParameter('TARGETHOST', 'TARGETHOST for this cluster')
        if (useTwoTargethosts) {
            addClusterReferenceParameter('TARGETHOST2', 'TARGETHOST2 for this cluster')
        }
        if (!withInstallNodePool){
            addClusterReferenceParameter('INSTALLNODE', 'InstallNode for this cluster. The ' +
                    'Installnode which performs the installation.')
        }
        job.with {
            parameters {
                stringParam('STAGING', 'LATEST', 'LATEST or a specific version will be fetched from ARM. Or a' +
                            ' file specified will be used, the file must be accessable from ' +
                            'Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL. Note!' +
                            ' include_playlists.ini must point to correct playlist version ' +
                            '(SUF_PACKAGE_NAME in TPG section).')
                if (useTwoTargethosts) {
                    stringParam('STAGING2', 'LATEST', 'LATEST or a specific version will be fetched from ARM. Or a' +
                            ' file specified will be used, the file must be accessable from ' +
                            'Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL. Note!' +
                            ' include_playlists.ini must point to correct playlist version ' +
                            '(SUF_PACKAGE_NAME in TPG section).')
                }
                stringParam('PLAYLISTS', 'LATEST', 'LATEST or a specific version will be fetched from ARM. Or a' +
                            ' file specified will be used, the file must be accessable from ' +
                            'Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL.')
                if (useTwoTargethosts){
                    stringParam('PLAYLISTS2', 'LATEST', 'LATEST or a specific version will be fetched from ARM. Or a' +
                        ' file specified will be used, the file must be accessable from ' +
                        'Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL.')
                }
                stringParam('STAGINGDEPS', '', 'Optional. Use to override deps dowloaded with staging directory. The file must be accessable from Jenkins as' +
                        ' a filepath (ie /workarea/.. or /proj/..) or a URL. LATEST or a specific version will be fetched from ARM.')
                if (useTwoTargethosts) {
                    stringParam('STAGINGDEPS2', '', 'Optional. Use to override deps dowloaded with staging directory. The file must be accessable from ' +
                            'Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL. LATEST or a specific version will be fetched from ARM.')
                }
                stringParam('TASKDIR', '', "Optional. If standard tasks should not be used. To give multiple dirs separate with space. Defaults to " +
                        "'/proj/env/tapastasks/'.")
                stringParam('CONFIGDIR', "/proj/eta-automation/tapas/config/" + projectName + "/config/",
                        'The location where the umi_test.xml file is located.')
                stringParam('VMAPIPROFILE', tpgName + '.', 'Specifies which credentials to use in VCenter.<br>Note: It should end with a dot!')
                stringParam('HOSTPROFILE', '', 'Specifies machine specific properties: vmfolder, hypervisorname, datastorename and network. If empty it will' +
                        ' match on just TARGETHOST.<br>Note: It should end with a dot!')
                stringParam('RPM', '', 'Optional. Specifies the rpm to use for TARGETHOST. The file must be accessable from Jenkins as a filepath (ie ' +
                        '/workarea/.. or /proj/..) or a URL.')
                if (useTwoTargethosts) {
                    stringParam('RPM2', '', 'Optional. Specifies the rpm to use for TARGETHOST2. The file must be accessable from Jenkins as a filepath (ie ' +
                            '/workarea/.. or /proj/..) or a URL.')
                }
            }

            getOptionalInputParameters()

            parameters {
                stringParam('JIVEVERSION', 'LATEST', 'Path to jive artifact, The file must be accessable from Jenkins as a filepath (ie /workarea/.. or ' +
                        '/proj/..) or a URL.')
                choiceParam('DO_ROLLBACK', ['true', 'false'], 'If true MSV and CIL will be rollbacked to snapshots.')
                choiceParam('NGEEVERSION', ['2', '1'], 'The version of NGEE being installed.')
            }
        }

        configureBuildEnvironment().failTheBuildIfItStuck(240).runXvfbDuringBuild()
        setPreBuildParameters()
        setEnvVariables()
        setPostBuildParameters()
        if (withInstallNodePool) {
            setParameterPool(jobName, 'INSTALLNODE', installNodePool)
        }
    }

    protected void setPreBuildParameters() {
        job.with {
            wrappers {
                preBuildCleanup()
            }
        }
    }

    protected void setPostBuildParameters(){
        job.with {
            publishers {
                postBuildScripts {
                    onlyIfBuildSucceeds(false)
                    steps {
                        environmentVariables {
                            propertiesFile('env.properties')
                        }
                    }
                }
            }
        }
    }

    protected String getOptionalTapasParameters() {
        return ""
    }

    protected void setEnvVariables() {
        Map env_list = getInjectVariables()
        env_list.remove("HOME")
        injectEnv(env_list)
    }

    protected String getTapasParameters()
    {
        def params = '--define=__INSTALLNODE__=\${INSTALLNODE} \\\n'
        params += '--define=__MSV__=\${MSV} \\\n'
        if (useCil) {
            params += '--define=__CIL__=\${CIL} \\\n'
        }
        params += '--define=__TARGETHOST__=\${TARGETHOST} \\\n'
        params += '--define=__STAGING__=\${STAGING} \\\n'
        params += '--define=__PLAYLISTS__=\${PLAYLISTS} \\\n'
        params += '--define=__STAGINGDEPS__="\${STAGINGDEPS}" \\\n'
        params += '--define=__VMAPI_PROFILE_PREFIX__=\${VMAPIPROFILE} \\\n'
        params += '--define=__HOST_PROFILE_PREFIX__="\${HOSTPROFILE}" \\\n'
        params += '--define=__HOST_CONFIG_DIR__=\"${CONFIGDIR}" \\\n'
        params += '--define=__PRODUCT_CONFIG_DIR__=\"${CONFIGDIR}" \\\n'
        params += '--define=__RPM__="\${RPM}" \\\n'
        if (useTwoTargethosts) {
            params += '--define=__TARGETHOST2__=\${TARGETHOST2} \\\n'
            params += '--define=__STAGING2__=\${STAGING2} \\\n'
            params += '--define=__STAGINGDEPS2__="\${STAGINGDEPS2}" \\\n'
            params += '--define=__RPM2__="\${RPM2}" \\\n'
            params += '--define=__PLAYLISTS2__=\${PLAYLISTS2} \\\n'
        }
        params += getOptionalTapasParameters()
        params += '--define=__JIVEVERSION__=${JIVEVERSION} \\\n'
        params += '--define=__DO_ROLLBACK__=\${DO_ROLLBACK} \\\n'
        params += '--define=__NGEEVERSION__=\${NGEEVERSION} \\\n'
        return params
    }

    protected void getOptionalInputParameters() {
    }

}
