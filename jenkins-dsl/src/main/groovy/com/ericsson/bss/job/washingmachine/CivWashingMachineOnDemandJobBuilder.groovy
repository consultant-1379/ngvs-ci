package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.AbstractTapasJobBuilder
import javaposse.jobdsl.dsl.Job

class CivWashingMachineOnDemandJobBuilder extends AbstractTapasJobBuilder {

    protected List tpgData = [
                                  [
                                      tpg: 'RMCA',
                                      url: 'https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-release-local/com/ericsson/bss/RMCA/,' +
                                           'https://arm.epk.ericsson.se/artifactory/simple/proj-rmca-release-local/com/ericsson/bss/rmca/integration/' +
                                           'rmcapackage/;24.2.0',
                                      suitePath: 'suites/washingmachine.xml',
                                      extraTapasParameter: '--define=__DISPLAYHOME__=${WORKSPACE} ' +
                                                           '--define=__OVFVERSION__=${TPG_VERSION} ' +
                                                           '--define=__EXECUTION_HOST_PORT__=${EXECUTION_HOST_PORT}'
                                  ],
                                  [
                                      tpg: 'CPM',
                                      url: 'https://arm.epk.ericsson.se/artifactory/proj-cpm-release-local/com/ericsson/bss/rm/cpm/umi/cpm/;1.4.0',
                                      suitePath: 'suites/installnode/washingmachine.xml',
                                      extraTapasParameter: '--define=__FOLDER__="snapshot" ' +
                                                           '--define=__OVFVERSION__=${TPG_VERSION} '
                                  ],
                                  [
                                      tpg: 'Charging',
                                      url: 'https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/ericsson/bss/rm/charging.core/;1.5.0',
                                      suitePath: 'suites/installnode/washingmachine_ovf.xml',
                                      extraTapasParameter: '--define=__TARGETHOST2__=${TARGETHOST2} ' +
                                                           '--define=__DLBHOST__=${DLBHOST} ' +
                                                           '--define=__COREOVFVERSION__=${TPG_VERSION} ' +
                                                           '--define=__ACCESSOVFVERSION__=${TPG_VERSION} ' +
                                                           '--define=__DLBOVFVERSION__=${TPG_VERSION} ' +
                                                           '--define=__JIVEVERSION__=${TPG_VERSION} ' +
                                                           '--define=__INCLUDE_DLB__=${useDlbHost}'
                                  ],
                             ]

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        setDescription()
        discardOldBuilds(30, 30,)
        addBuildParameters()
        setConcurrentBuild(false)
        deleteWorkspaceBeforeBuildStarts()
        addTimeoutAndAbortConfig(240)
        setEnvironmentVariables()
        deleteWorkspaceAfterBuild()

        return job
    }

    protected void addBuildParameters() {
        List tpgs = []
        for (int i = 0; i < tpgData.size(); ++i) {
            tpgs.add(tpgData[i].tpg)
        }

        addListChoiceParam('TPG', tpgs, 'Which TPG should be installed.')
        addActiveChoiceReactiveParam('TPG_VERSION', 'SINGLE_SELECT', getVersionScript('TPG'), defaultFallbackScript(),
                                     ['TPG'], false, 'Which version should be installed for the selected TPG')
        addStringParam('RMBASEKARAF_FILE', '', '<b>[Optional]</b> Specify if a custom RMBASE image should be used.')
        addVersionChoiceParam('RMBASEKARAF_VERSION', 'https://arm.epk.ericsson.se/artifactory/simple/proj-bssf-dev/com/ericsson/bss/umi/rmbase/',
                              'Specify if a custom release version should be used.<br>\nEg. "x.y.z" or "x.y.z-SNAPSHOT"')
        addStringParam('MSV', '', 'The MSV machine to use.')
        addStringParam('CIL', '', 'The CIL machine to use.')
        addStringParam('TARGETHOST', '', 'The machine which should be used as Targethost.')
        addReferenceParameter('TARGETHOST2', 'TPG', 'The machine which should be used as Targethost2')
        addReferenceParameter('DLBHOST', 'TPG', 'The machine which should be used as a DLB HOST. If empty the DLB will not be used.')
        addStringParam('VMAPI_PREFIX', projectName + '.', 'To use correct credentitials and vCenter. Normally the product the host belongs to.<br>\n' +
                                                          'Ex: "cil.", "charging.", "cpm.".')
        addStringParam('PRODUCT', projectName, 'The product the machine belongs to, normally the same as VMAPI_PREFIX (but WITHOUT the ending dot).<br>\n' +
                                               'Ex: "rmca", "charging", "cil", "cpm", "coba", "ss7translator", "invoicing".')
        addStringParam('HOST_PREFIX', '', '<b>[Optional]</b> If the host should be located in a specfic folder ' +
                                          'in the Resource pool, then add the host prefix here with an ending dot.')
    }

    protected String getVersionScript(String referencedParamter) {
        String version_script = 'version_list = []\n'

        for (int i = 0; i < tpgData.size(); ++i) {
            version_script += "if (${referencedParamter} == '${tpgData[i].tpg}') {\n" +
                              "    metadataPath = '${tpgData[i].url}'\n" +
                              "}\n"
        }
        version_script += '\n' +
                          'try {\n' +
                          '    files = metadataPath.tokenize(\',\')\n' +
                          '    for (String file : files) {\n' +
                          '        (path, fromversion) = file.tokenize(\';\')\n' +
                          '        metadata = new XmlSlurper().parse(path + "/maven-metadata.xml")\n' +
                          '        metadata.versioning.versions.version.each{\n' +
                          '            if (!fromversion || includeVersion(fromversion, it.text())) {\n' +
                          '                version_list.add(it.text())\n' +
                          '            }\n' +
                          '        }\n' +
                          '    }\n' +
                          '} catch (IOException e) {}\n' +
                          '\n' +
                          'version_list.sort{a, b-> a == b ? 0 : includeVersion(b, a) ? -1 : 1}\n' +
                          'version_list.add(0, "LATEST:selected")\n' +
                          '\n' +
                          'return version_list\n' +
                          '\n' +
                          '/*\n' +
                          ' * The sorting algorith will first run a 2 part tokenizer\n' +
                          ' * splitting away anything after a "-" for a string compare\n' +
                          ' * and every "." for an integral compare.\n' +
                          ' * \n' +
                          ' * Each version part (major, minor, micro) is compared in\n' +
                          ' * order and returns an early result on missmatch.\n' +
                          ' * If all version parts are equal we run the string compare\n' +
                          ' * if available (both version strings contain a "-") and\n' +
                          ' * return the result.\n' +
                          ' *\n' +
                          '*/\n' +
                          'boolean includeVersion(String limitVer, String requestVer) {\n' +
                          '    limitrcpart = null\n' +
                          '    requestrcpart = null\n' +
                          '    limitParts = ""\n' +
                          '    requestParts = ""\n' +
                          '\n' +
                          '    if (limitVer.contains("-") && requestVer.contains("-")) {\n' +
                          '        limitrcparts = limitVer.tokenize("-")\n' +
                          '        requestrcparts = requestVer.tokenize("-")\n' +
                          '        limitrcpart = limitrcparts[1]\n' +
                          '        requestrcpart = requestrcparts[1]\n' +
                          '        limitParts = limitrcparts[0].tokenize(".")\n' +
                          '        requestParts = requestrcparts[0].tokenize(".")\n' +
                          '    } else {\n' +
                          '        limitParts = limitVer.tokenize(".")\n' +
                          '        requestParts = requestVer.tokenize(".")\n' +
                          '    }\n' +
                          '\n' +
                          '    for (int i = 0; i < 3; i++) {\n' +
                          '        try {' +
                          '            limitNum = limitParts[i].toInteger()\n' +
                          '            requestNum = requestParts[i].toInteger()\n' +
                          '\n' +
                          '            if (requestNum < limitNum) {\n' +
                          '                return false\n' +
                          '            }\n' +
                          '            else if (requestNum > limitNum) {\n' +
                          '                return true\n' +
                          '            }\n' +
                          '        }\n' +
                          '        catch (Exception e) {\n' +
                          '            return false\n' +
                          '        }\n' +
                          '    }\n' +
                          '    if (limitrcpart && requestrcpart) {\n' +
                          '        return limitrcpart.compareTo(requestrcpart)\n' +
                          '    }\n' +
                          '    return true\n' +
                          '}\n'
        return version_script
    }

    protected void setEnvironmentVariables() {
        Map env_list = getInjectVariables()
        env_list.remove("HOME")
        injectEnv(env_list)
    }

    @Override
    protected void setTapasShell() {
        job.with {
            steps {
                shell(symlinkMesosWorkSpace())
                shell(dslFactory.readFileFromWorkspace('scripts/washingmachine/cpm_washingmachine_pre_tapas.sh'))
                shell(getTapasShell())
            }
        }
    }

    @Override
    protected String getReferenceParameterScript(String referenceParameter) {
        return 'if (' + referenceParameter + '.equals("Charging")) {\n' +
               '    return "<input name=\\"value\\" value=\\"\\" class=\\"setting-input\\" type=\\"text' +
               '\\">"\n' +
               ' } else {\n' +
               '   return "<input name=\\"value\\" value=\\"\\" class=\\"setting-input\\" type=\\"text' +
               '\\" disabled>"\n' +
               '}\n'
    }

    @Override
    protected String getAdditionalMultipleHostShell() {
        String suitePath = "\n"
        for (int i = 0; i < tpgData.size(); ++i) {
            suitePath += 'if [[ ${TPG} == "' + tpgData[i].tpg + '" ]]; then\n' +
                         '    suitePath="' + tpgData[i].suitePath + '"\n' +
                         'fi\n'
        }
        return suitePath + '\n'
    }

    @Override
    protected String getAdditionalTapasShell() {
        String additionalShell = super.getAdditionalTapasShell() + '\n\n' +
                                 'tpgSpecificParameters=""\n'
        for (int i = 0; i < tpgData.size(); ++i) {
            additionalShell += 'if [[ ${TPG} == "' + tpgData[i].tpg + '" ]]; then\n' +
                               '    tpgSpecificParameters="' + tpgData[i].extraTapasParameter + '"\n' +
                               'fi\n'
        }

        additionalShell += '\n' +
                           'if [[ ${DLBHOST} == "" ]]; then\n' +
                           '    useDlbHost="false"\n' +
                           'else\n' +
                           '    useDlbHost="true"\n' +
                           'fi\n'

        return additionalShell + '\n'
    }

    @Override
    protected String getTapasParameters() {
        String paramters = super.getTapasParameters() +
                           '--define=__NAME__="CIV ${TPG} Washingmachine" \\\n' +
                           '--define=__PROJECT__="CIV" \\\n' +
                           '--define=__RMBASECUSTOMFILE__=${RMBASE_VERSION} \\\n' +
                           '--define=__RMBASECUSTOMVERSION__=${RMBASEKARAF_VERSION} \\\n' +
                           '$tpgSpecificParameters \\\n'
        return paramters
    }
}
