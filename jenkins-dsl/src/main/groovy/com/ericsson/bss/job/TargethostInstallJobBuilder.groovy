package com.ericsson.bss.job

import com.ericsson.bss.AbstractTapasJobBuilder
import com.ericsson.bss.util.Email

import javaposse.jobdsl.dsl.Job

public class TargethostInstallJobBuilder extends AbstractTapasJobBuilder {

    protected List<String> resourceProfiles = []
    protected HashMap<String, List> valuesOfResourceProfiles = new HashMap<String, List>()
    protected String resourceProfilesDescription = ""
    protected String extraDescription = ""
    protected List versionLocation = []
    protected String installNodeName = ""
    protected boolean useCil = true
    protected List targethostDescription = []
    protected boolean useDvFile = false
    protected boolean useJiveTests = false
    protected String jiveMetaData = ""
    protected boolean useSeleniumTests = false
    protected String seleniumMetaData = ""
    protected List booleanTrueFalseList = ['true', 'false']
    protected List booleanFalseTrueList = ['false', 'true']
    protected boolean useResourceProfile = true
    protected boolean useTestData = false
    protected String testdataVersionLocation = ""
    protected int nrOfNetworks = 1

    protected List installType = []
    protected List ovfPacName = []
    protected boolean useAppGroup = false
    protected String suiteTwoHosts
    protected String suiteFileTwoHost
    protected boolean useMultipleCils = false
    protected boolean useMultipleTargethosts = true

    public Job build() {
        super.buildDescription = "\$TARGETHOST"
        suiteFile = suite.split ('/')[-1].replace('.xml', '_\${TARGETHOST}.xml')
        suiteFileTwoHost = suiteTwoHosts.split ('/')[-1].replace('.xml', '_\${TARGETHOST}-\${TARGETHOST2}.xml')
        initProject(dslFactory.freeStyleJob(jobName))
        setDescription("<br>\n" +
                       "This job is used to install " + projectName + " on a team cluster.<br>\n" +
                       extraDescription)

        addTimeoutAndAbortConfig(timeoutForJob)
        deleteWorkspaceBeforeBuildStarts()
        addPostBuildScripts()

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
        if (versionLocation.size() > 1) {
            job.with {
                parameters {
                    choiceParam('INSTALLTYPE', installType, 'Which type of installation to be performed.')
                }
            }
        }
        addSelectClusterParameter('Choose cluster to install')

        addClusterReferenceParameter('TARGETHOST', targethostDescription[0])

        addVersionChoiceParam('VERSION', versionLocation[0],
                              'Version of ' + projectName + ' OVF to install. Use LATEST to get the latest working SNAPSHOT OVF from Washingmachine.')
        for (int i = 1; i < versionLocation.size(); i++) {
            int versionNumber = i + 1
            addClusterReferenceParameter('TARGETHOST' + versionNumber, targethostDescription[i])

            addVersionChoiceParam('VERSION' + versionNumber, versionLocation[i],
                              'Version ' + versionNumber + ' of ' + projectName + ' OVF to install. Use LATEST to get the latest ' +
                                                                            'working SNAPSHOT OVF from Washingmachine.')

        }

        addClusterReferenceParameter('MSV', 'The MSV for this cluster')
        if (useCil) {
            addClusterReferenceParameter('CIL', 'The CIL for this cluster')
        }
        job.with {
            parameters {
                if (useDvFile) {
                    stringParam('DVFILE', '',
                                'Optional. A custom file to use instead of an artifact from ARM (' + projectName + '-*-dv.tar.gz). ' +
                                'The file must be accessible from Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL. ' +
                                'Leave empty to download the specified VERSION from ARM.')
                    for (int i = 2; i < versionLocation.size() + 1; i++) {
                        stringParam('DVFILE' + i, '',
                                    'Optional. A custom file to use instead of an artifact from ARM (' + projectName + '-*-dv.tar.gz). ' +
                                    'The file must be accessible from Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL. ' +
                                    'Leave empty to download the specified VERSION2 from ARM.')
                    }

                }
                if (useResourceProfile) {
                    choiceParam('RESOURCE_PROFILE', resourceProfiles, resourceProfilesDescription)
                }
                stringParam('VMAPI_PREFIX', projectName + '.',
                            'To use correct credentitials and vCenter. Normally the product the host belongs to. Ex: "cil.", "charging.", "cpm.".')
                stringParam('PRODUCT', projectName, 'The product the machine belongs to, normally the same as VMAPI_PREFIX (but WITHOUT the ending dot).' +
                                                    ' Ex: "rmca", "charging", "cil", "cpm", "coba", "ss7translator", "invoicing".')
                stringParam('HOST_PREFIX', '', '<b>[Optional]</b> If the host should be located in a specfic folder ' +
                                               'in the Resource pool, then add the host prefix here with an ending dot.')
                choiceParam('DO_ROLLBACK', booleanTrueFalseList, 'If true MSV and CIL will be rollbacked to snapshots. ' +
                                                              'Primarially used for adding ' + projectName + ' host to a existing cluster for testing.')
                choiceParam('RETRIES', getNumberedList(0, 3), 'The number of times the job should retry if failed. 0 means just to run the job ' +
                        'once.')
                if (nrOfNetworks > 1) {
                    choiceParam('NUM_OF_NETWORK', getNumberedList(1, nrOfNetworks),
                            'Number of network interfaces (vNIC) to deploy. Default is only one network interface (vNIC). It will take network information ' +
                            'available in IPDB')
                }

                activeChoiceReactiveReferenceParam('USERMAIL') {
                    description('This is a hidden parameter used for passing a list ' +
                                'of email addresses to which the notifications ' +
                                'will be sent.')
                    omitValueField()
                    choiceType('FORMATTED_HIDDEN_HTML')
                    groovyScript {
                        script('return ""')
                        fallbackScript(defaultFallbackScript())
                    }
                }
            }
        }

        if (useAppGroup) {
            job.with {
                parameters {
                    stringParam('APPGROUP', projectName.toUpperCase() + '1', 'What appgroup should be used.')
                }
            }
        }

        if (useTestData) {
            addListChoiceParam('INSTALL_TESTDATA', booleanTrueFalseList, 'Should test data be installed.')
            addVersionChoiceParam('AVAILABLE_TESTDATA_VERSIONS', testdataVersionLocation, 'Testdata version for ' + projectName + ' to use.', true)
            addReferenceParameter('TESTDATA_VERSION', 'AVAILABLE_TESTDATA_VERSIONS', 'Select a TestData version from above or supply your own from a ' +
                'location Jenkins can access, ie: http:// or /proj/...')
        }
        if (useJiveTests) {
            addListChoiceParam('RUN_JIVE_TESTS', booleanFalseTrueList, 'Should Jive tests be executed after installation')
            addVersionChoiceParam('AVAILABLE_JIVE_VERSIONS', jiveMetaData, 'The Jive version to use. ', true)
            addReferenceParameter('JIVE_VERSION', 'AVAILABLE_JIVE_VERSIONS', 'Select a Jive version from above or supply your own from a location Jenkins ' +
                'can access, ie: http:// or /proj/...')
        }
        if (useSeleniumTests) {
            addListChoiceParam('RUN_SELENIUM_TESTS', booleanFalseTrueList, 'Should Jive tests be executed after installation')
            addVersionChoiceParam('AVAILABLE_SELENIUM_VERSIONS', seleniumMetaData, 'The Selenium version to use. ', true)
            addReferenceParameter('SELENIUM_VERSION', 'AVAILABLE_SELENIUM_VERSIONS', 'Select a Selenium version from above or supply your own file. The ' +
                'file must be accessable from Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL.')
        }
        job.with {
            parameters {
                stringParam('PRODUCT_CONFIG_DIR', '/proj/eta-automation/tapas/config/' + projectName + '/',
                        'Optional. Only change this if you have a custom product config dir for suite or other config files (except hosts).')
            }
        }
        setExtraOptions()
    }

    protected void setExtraOptions() {
        return
    }

    @Override
    protected String getAdditionalMultipleHostShell() {
        String multihostShell = ""

        if (!useDvFile) {
            multihostShell += '\n' +
                              'if [[ "${VERSION}" = "LATEST" ]]; then\n' +
                              '    VERSION="999.9.9"\n' +
                              'fi\n'

            for (int i = 2; i < versionLocation.size() + 1; i++) {
            multihostShell += 'if [[ "${VERSION' + i + '}" = "LATEST" ]]; then\n' +
                              '    VERSION' + i + '="999.9.9"\n' +
                              'fi\n'
            }

        }

        if (useCil & useMultipleCils) {
            multihostShell += 'NUMBER_OF_CILS="$(echo ${CIL} | awk --field-separator=";" \'{ printf NF }\')"\n' +
                              'CILS_JSON="$(printf \'[%s]\' $(echo ${CIL} | sed -e \'s/;/\\n/g\' | xargs printf \'{\\"target.cil\\"' +
                              ':\\"%s\\"},\' | sed s\'/.$//\'))\" \n' +
                              'echo "CILS_JSON: ${CILS_JSON}"\n'
            if (useJiveTests) {
                multihostShell += 'export JIVE_CIL="$(echo ${CIL} | awk --field-separator=";" \'{ printf $1 }\')"\n' +
                                  'echo "JIVE_CIL: $JIVE_CIL"\n' +
                                  '\n'
            }
        }

        if (useJiveTests) {
            multihostShell += 'export JIVE_TARGETHOST="$(echo ${TARGETHOST} | awk --field-separator=";" \'{ printf $1 }\')"\n' +
                              'echo "JIVE_TARGETHOST: $JIVE_TARGETHOST"\n'
        }

        if (versionLocation.size() >  1) {
            if (useJiveTests) {
                multihostShell += 'export JIVE_TARGETHOST2="$(echo ${TARGETHOST2} | awk --field-separator=";" \'{ printf $1 }\')"\n' +
                                  'echo "JIVE_TARGETHOST2: $JIVE_TARGETHOST2"\n' +
                                  '\n'
            }
            if (useResourceProfile) {
                multihostShell += '# Empty values = use the defaults specified\n' +
                'if [ "' + resourceProfiles[0] + '" == "${RESOURCE_PROFILE}" ]; then\n' +
                '    CUSTOMCPU="' + valuesOfResourceProfiles[resourceProfiles[0]][0] + '"\n' +
                '    CUSTOMRAM="' + valuesOfResourceProfiles[resourceProfiles[0]][1] + '"\n'
                for (int i = 1; i < versionLocation.size();i++) {
                    multihostShell += '    CUSTOMCPU' + (i + 1) + '="' + valuesOfResourceProfiles[resourceProfiles[0]][2 * i] + '"\n' +
                                      '    CUSTOMRAM' + (i + 1) + '="' + valuesOfResourceProfiles[resourceProfiles[0]][2 * i + 1] + '"\n'
                }

                for (String it in resourceProfiles.subList( 1, resourceProfiles.size() ) ) {
                    List value = valuesOfResourceProfiles.get(it)
                    multihostShell += 'elif [ "' + it + '" == "${RESOURCE_PROFILE}" ]; then\n' +
                                               '    CUSTOMCPU="' + value[0] + '"\n' +
                                               '    CUSTOMRAM="' + value[1] + '"\n'
                                               for (int i = 1; i < versionLocation.size();i++) {
                                                   multihostShell += '    CUSTOMCPU' + (i + 1) + '="' + value[2 * i] + '"\n' +
                                                                     '    CUSTOMRAM' + (i + 1) + '="' + value[2 * i + 1] + '"\n'
                                               }
                }
                multihostShell += 'fi\n'
            }

            multihostShell += 'HOST_CONFIG_DIR="/proj/eta-automation/tapas/config/${PRODUCT}/config/"\n' +
                              '\n'
            multihostShell += hostResourceProfile()
        }
        return multihostShell
    }

    protected String hostResourceProfile() {
        String resourceProfileShell = 'if [[ "${INSTALLTYPE}" = "' + installType[-1] + '" ]]; then\n'
        if (useResourceProfile) {
            resourceProfileShell += '    CUSTOMRAM="${CUSTOMRAM2}" \n' +
                                    '    CUSTOMCPU="${CUSTOMCPU2}"\n'
        }
        resourceProfileShell +=   '    OVFPACNAME="' + ovfPacName[1] + '"\n' +
                            '    VERSION=${VERSION2}\n' +
                            '\n'
        if (useMultipleTargethosts) {
            resourceProfileShell += '    NUMBER_OF_TARGETHOSTS="$(echo ${TARGETHOST2} | awk --field-separator=";" \'{ printf NF }\')"\n' +
                                    '    NUMBER_OF_TARGETHOSTS2="$(echo ${TARGETHOST} | awk --field-separator=";" \'{ printf NF }\')"\n' +
                                    '\n' +
                                    '    TARGETHOSTS_JSON="$(printf \'[%s]\' $(echo ${TARGETHOST2} | sed -e \'s/;/\\n/g\' ' +
                                    '| xargs printf \'{\\"target.host\\":\\"%s\\"},\' | sed s\'/.$//\'))"\n' +
                                    '    TARGETHOSTS2_JSON="$(printf \'[%s]\' $(echo ${TARGETHOST} | sed -e \'s/;/\\n/g\' ' +
                                    '| xargs printf \'{\\"target.host2\\":\\"%s\\"},\' | sed s\'/.$//\'))"\n' +
                                    '\n'
        } else {
             resourceProfileShell +=   '    SWAP_TARGETHOSTS="${TARGETHOST}"\n' +
                                       '    TARGETHOST="${TARGETHOST2}"\n' +
                                       '    TARGETHOST2="${SWAP_TARGETHOSTS}"\n' +
                                       '\n'
        }
        resourceProfileShell +=   'else\n' +
                                  '    OVFPACNAME="' + ovfPacName[0] + '"\n' +
                                  '    VERSION=${VERSION}\n' +
                                  '\n'
        if (useMultipleTargethosts) {
        resourceProfileShell +=   '    NUMBER_OF_TARGETHOSTS="$(echo ${TARGETHOST} | awk --field-separator=";" \'{ printf NF }\')"\n' +
                                  '    NUMBER_OF_TARGETHOSTS2="$(echo ${TARGETHOST2} | awk --field-separator=";" \'{ printf NF }\')"\n' +
                                  '\n' +
                                  '    TARGETHOSTS_JSON="$(printf \'[%s]\' $(echo ${TARGETHOST} | sed -e \'s/;/\\n/g\' ' +
                                  '| xargs printf \'{\\"target.host\\":\\"%s\\"},\' | sed s\'/.$//\'))"\n' +
                                  '    TARGETHOSTS2_JSON="$(printf \'[%s]\' $(echo ${TARGETHOST2} | sed -e \'s/;/\\n/g\' ' +
                                  '| xargs printf \'{\\"target.host2\\":\\"%s\\"},\' | sed s\'/.$//\'))"\n'
        }
        resourceProfileShell +=   'fi\n' +
                                  '\n'
        if (useMultipleTargethosts) {
            resourceProfileShell += 'echo "TARGETHOSTS_JSON: ${TARGETHOSTS_JSON}"\n'
            for (int i = 2; i < versionLocation.size() + 1;i++) {
                resourceProfileShell += 'echo "TARGETHOSTS' + i + '_JSON ${TARGETHOSTS' + i + '_JSON}"\n'
            }
            resourceProfileShell += '\n'
        }
        return resourceProfileShell
    }

    @Override
    protected String getAdditionalTapasShell() {
        String additional_tapas_shell = ""
        if (versionLocation.size() > 1) {
            if (useCil & useMultipleCils) {
                additional_tapas_shell += 'CIL=${CILS_JSON}\n'
            }
            if (useMultipleTargethosts) {
                additional_tapas_shell += 'TARGETHOST=${TARGETHOSTS_JSON}\n'
                for (int i = 2; i < versionLocation.size() + 1; i++) {
                    additional_tapas_shell += 'TARGETHOST' + i + '=${TARGETHOSTS' + i + '_JSON}\n'
                }
            }

            return additional_tapas_shell
        }

        additional_tapas_shell = 'HOST_CONFIG_DIR="/proj/eta-automation/tapas/config/${PRODUCT}/config/"\n'

        if (useResourceProfile) {
            additional_tapas_shell += '\n'+
                   '# Empty values = use the defaults specified in MSV ALL OVF\n' +
                   'if [ "' + resourceProfiles[0] + '" == "${RESOURCE_PROFILE}" ]; then\n' +
                   '    CUSTOMCPU="' + valuesOfResourceProfiles[resourceProfiles[0]][0] + '"\n' +
                   '    CUSTOMRAM="' + valuesOfResourceProfiles[resourceProfiles[0]][1] + '"\n'

                   for (String it in resourceProfiles.subList( 1, resourceProfiles.size() ) ) {
                       List value = valuesOfResourceProfiles.get(it)
                       additional_tapas_shell += 'elif [ "' + it + '" == "${RESOURCE_PROFILE}" ]; then\n' +
                                                  '    CUSTOMCPU="' + value[0] + '"\n' +
                                                  '    CUSTOMRAM="' + value[1] + '"\n'
                   }

                   additional_tapas_shell +=  'fi\n' +
                                               '\n'
        }

        if (useJiveTests || useSeleniumTests) {
            additional_tapas_shell += '\n'+
                                       'EXECUTION_HOST_PORT=$((${ALLOCATED_PORT}+1))\n'
        }
        return additional_tapas_shell
    }

    @Override
    protected String getTapasParameters() {
        String params = '--define=__TARGETHOST__=${TARGETHOST} \\\n' +
                        '--define=__OVFVERSION__="${VERSION}" \\\n'
        if (useResourceProfile) {
            params +=   '--define=__CUSTOMCPU__="${CUSTOMCPU}" \\\n' +
                        '--define=__CUSTOMRAM__="${CUSTOMRAM}" \\\n'
        }

            for (int i = 2; i < versionLocation.size() + 1; i++) {
                params +=   '--define=__TARGETHOST' + i + '__=${TARGETHOST' + i + '} \\\n' +
                            '--define=__OVFVERSION' + i + '__="${VERSION' + i + '}" \\\n'
                if (useResourceProfile) {
                    params += '--define=__CUSTOMCPU' + i + '__="${CUSTOMCPU' + i + '}" \\\n' +
                              '--define=__CUSTOMRAM' + i + '__="${CUSTOMRAM' + i + '}" \\\n'
                }
            }

        params +=       '--define=__MSV__=${MSV} \\\n'
        if (useCil) {
            params +=   '--define=__CIL__=\${CIL} \\\n'
            if (useMultipleCils) {
                params +=   '--define=__NUMBER_OF_CILS__=\${NUMBER_OF_CILS} \\\n'
            }
        }
        if (useDvFile) {
            params +=   '--define=__DVFILE__=\${DVFILE} \\\n'

            for (int i = 2; i < versionLocation.size() + 1; i++) {
                params +=   '--define=__DVFILE' + i + '__=\${DVFILE' + i + '} \\\n'
            }

        }
        else{
            params +=   '--define=__INSTALLNODE__=' + installNodeName + ' \\\n'
        }
        params +=       '--define=__VMAPI_PROFILE_PREFIX__=${VMAPI_PREFIX} \\\n' +
                        '--define=__HOST_PROFILE_PREFIX__=${HOST_PREFIX} \\\n' +
                        '--define=__HOST_CONFIG_DIR__=${HOST_CONFIG_DIR} \\\n' +
                        '--define=__DO_ROLLBACK__=${DO_ROLLBACK} \\\n'
        if (useTestData) {
            params +=   '--define=__INSTALL_TESTDATA__=\${INSTALL_TESTDATA} \\\n'
            params +=   '--define=__TESTDATA_VERSION__=\${TESTDATA_VERSION} \\\n'
        }
        if (nrOfNetworks > 1) {
            params +=   '--define=__NUM_OF_NETWORK__="${NUM_OF_NETWORK}" \\\n'
        }
        if (useJiveTests) {
            params +=   '--define=__RUN_JIVE_TESTS__=\${RUN_JIVE_TESTS} \\\n' +
                        '--define=__JIVE_VERSION__=\${JIVE_VERSION} \\\n' +
                        '--define=__JIVE_TARGETHOST__=\${JIVE_TARGETHOST} \\\n'
            if (versionLocation.size() >  1) {
                params += '--define=__JIVE_TARGETHOST2__=\${JIVE_TARGETHOST2} \\\n'
            }
            if (useMultipleCils) {
                params += '--define=__JIVE_CIL__=\${JIVE_CIL} \\\n'
            }
        }
        if (useSeleniumTests) {
            params +=   '--define=__RUN_SELENIUM_TESTS__=\${RUN_SELENIUM_TESTS} \\\n' +
                        '--define=__SELENIUM_VERSION__=\${SELENIUM_VERSION} \\\n'
        }
        if (useJiveTests || useSeleniumTests) {
            params +=   '--define=__EXECUTION_HOST_PORT__=\${EXECUTION_HOST_PORT} \\\n'
        }
        if (useAppGroup) {
            params +=   '--define=__APPGROUP__=\${APPGROUP} \\\n'
        }
        params += '--define=__PRODUCT_CONFIG_DIR__="\${PRODUCT_CONFIG_DIR}config/" \\\n'
        params += getOptionalTapasParameters()
        return params
    }

    protected String getOptionalTapasParameters() {
        String additionalParameters = ""
        if (versionLocation.size() > 1) {
            additionalParameters = '--define=__OVFPACNAME__=${OVFPACNAME} \\\n' +
                                   '--define=__VARIANT__=${INSTALLTYPE} \\\n'
            if (useMultipleTargethosts) {
                additionalParameters += '--define=__NUMBER_OF_TARGETHOSTS__=${NUMBER_OF_TARGETHOSTS} \\\n'
                for (int i = 2; i < versionLocation.size() + 1; i++) {
                    additionalParameters += '--define=__NUMBER_OF_TARGETHOSTS' + i + '__=${NUMBER_OF_TARGETHOSTS' + i + '} \\\n'
                }
            }
        }
        return additionalParameters
    }

    @Override
    protected String getAdditionalFinishingTapasShell() {
        return  '\n' +
                'SUCCESS="SUCCESS"\n' +
                'echo "SUCCESS=\$SUCCESS" >> env.properties\n' +
                'echo \$SUCCESS\n'
    }

    protected void addPostBuildScripts() {
        job.with {
            publishers {
                postBuildScripts {
                    steps {
                        shell( getUserMailShell() )
                        environmentVariables {
                            propertiesFile('env.properties')
                        }
                        conditionalSteps {
                            condition {
                                shell(getPostBuildShell())
                            }
                            steps {
                                downstreamParameterized {
                                    trigger(projectName + '_targethost_install') {
                                        parameters {
                                            currentBuild()
                                            propertiesFile('env.properties', false)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    onlyIfBuildSucceeds(false)
                }
            }
        }
    }

    protected String getUserMailShell() {
        return 'if ([ "${BUILD_USER_EMAIL}" = "" ] ' +
                '|| [ "${BUILD_USER_EMAIL}" = ' +
                '"kascmadm@noreply.epk.ericsson.se" ])' +
                ' && [ "${USERMAIL}" != "" ]; then\n' +
                '    BUILD_USER_EMAIL=$USERMAIL\n' +
                'fi\n' +
                'echo "BUILD_USER_EMAIL=$BUILD_USER_EMAIL" ' +
                '>> "env.properties"\n'
    }

    protected String getPostBuildShell() {
        return "echo \$SUCCESS\n" +
               'if [[ \$RETRIES = 0 || "\$SUCCESS" = "SUCCESS" ]]; then\n' +
               "  exit 1\n" +
               "else\n" +
               "  RETRIES=\$(((\$RETRIES-1)))\n" +
               '  echo "RETRIES=\$RETRIES" >> env.properties\n' +
               "  echo \$RETRIES\n" +
               "  exit 0\n" +
               "fi"
    }

    @Override
    protected void setExtraShell() {
        job.with {
            steps {
                shell("export TARGETHOST=\${TARGETHOST//,}")
            }
        }
    }

    @Override
    protected Email getAlwaysMailConfig()
    {
        String subject = '${TARGETHOST} - $DEFAULT_SUBJECT'
        if (versionLocation.size() > 1) {
            subject = '${TARGETHOST} - ${TARGETHOST2} - $DEFAULT_SUBJECT'
        }
        return Email.newBuilder().withRecipient('$DEFAULT_RECIPIENTS, $BUILD_USER_EMAIL')
                .withSubject(subject)
                .withContent('$DEFAULT_CONTENT')
                .withAlwaysTrigger()
                .build()
    }

    @Override
    protected String getTapasConfigSettings() {
        String config_shell = 'BASE_CONFIG_FILE="${PRODUCT_CONFIG_DIR}' + suite + '"\n' +
                              'CONFIG_FILE="/proj/eta-automation/tapas/sessions/' + tapasProjectDestinationName + '/' + suiteFile + '"\n'
        if (versionLocation.size() > 1) {
            config_shell +=   '\n' +
                              'if [[ "${INSTALLTYPE}" = "full" ]]; then\n' +
                              '    BASE_CONFIG_FILE="${PRODUCT_CONFIG_DIR}' + suiteTwoHosts + '"\n' +
                              '    CONFIG_FILE="/proj/eta-automation/tapas/sessions/' + tapasProjectDestinationName + '/' + suiteFileTwoHost + '"\n' +
                              'fi\n'
        }
        return config_shell
    }

}