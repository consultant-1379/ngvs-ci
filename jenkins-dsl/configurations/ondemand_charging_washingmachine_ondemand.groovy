projectDescription = ''
params = [
        ['name': 'TARGETHOST', 'type': 'string', 'defaultValue': '', 'description': 'Core host'],
        ['name': 'CORESTAGINGVERSION', 'type': 'string', 'defaultValue': 'LATEST', 'description': 'Version of the CHA core staging directory artifact (groupId: com.ericsson.bss.rm, artifactId: charging.core). Use LATEST to get the latest SNAPSHOT version.'],
        ['name': 'TARGETHOST2', 'type': 'string', 'defaultValue': '', 'description': 'Access host'],
        ['name': 'ACCESSSTAGINGVERSION', 'type': 'string', 'defaultValue': 'LATEST', 'description': 'Version of the CHA access staging directory artifact (groupId: com.ericsson.bss.rm, artifactId: charging.access). Use LATEST to get the latest SNAPSHOT version.'],
        ['name': 'DLBHOST', 'type': 'string', 'defaultValue': '', 'description': 'Dlb host'],
        ['name': 'DLBSTAGINGVERSION', 'type': 'string', 'defaultValue': 'LATEST', 'description': 'Version of the CHA dlb staging directory artifact ' +
                '(groupId: com.ericsson.bss.rm, artifactId: charging.dlb). Use LATEST to get the latest SNAPSHOT version.'],
        ['name': 'CIL', 'type': 'string', 'defaultValue': '', 'description': 'Cil host'],
        ['name': 'INSTALLNODE', 'type': 'string', 'defaultValue': 'vmeta011', 'description': ''],
        ['name': 'MSV', 'type': 'string', 'defaultValue': '', 'description': ''],
        ['name': 'JIVEVERSION', 'type': 'string', 'defaultValue': 'LATEST', 'description': 'Version of Jive test artifact (groupid:com.ericsson.jive.charging, artifactid:tests).<br>\n' +
                'You can also specify a custom Jive test artifact here. The file must be accessable from Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL.'],
        ['name': 'RPMCORE', 'type': 'string', 'defaultValue': '', 'description': 'Optional. Specifies a custom built CORE rpm to use. The file must be accessable from Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL.'],
        ['name': 'RPMACCESS', 'type': 'string', 'defaultValue': '', 'description': 'Optional. Specifies a custom built ACCESS rpm to use. The file must be accessable from Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL.'],
        ['name'               : 'ACCESSOVFVERSION',
         'type'               : 'active_choice',
         'description'        : 'If LATEST it will create a new OVF, otherwise take the specified version. <b>Note</b>, if not LATEST here the ACCESSSTAGINGVERSION will be skipped',
         'choiceType'         : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'ProjectVersionsScriptBuilder',
         'scriptBuilderParams': [
                 url       : 'https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/ericsson/bss/CHAACCESS/maven-metadata.xml',
                 withLatest: true
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'       : false
        ],
        ['name'               : 'COREOVFVERSION',
         'type'               : 'active_choice',
         'description'        : 'If LATEST it will create a new OVF, otherwise take the specified version. <b>Note</b>, if not LATEST here the CORESTAGINGVERSION will be skipped',
         'choiceType'         : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'ProjectVersionsScriptBuilder',
         'scriptBuilderParams': [
                 url       : 'https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/ericsson/bss/CHACORE/maven-metadata.xml',
                 withLatest: true
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'       : false
        ],
        ['name'               : 'DLBOVFVERSION',
         'type'               : 'active_choice',
         'description'        : 'If LATEST it will create a new OVF, otherwise take the specified version. <b>Note</b>, if not LATEST here the ' +
                 'DLBSTAGINGVERSION will be skipped',
         'choiceType'         : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'ProjectVersionsScriptBuilder',
         'scriptBuilderParams': [
                 url       : 'https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/ericsson/bss/CHADLB/maven-metadata.xml',
                 withLatest: true
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'       : false
        ],
        ['name': 'VMAPIPROFILE', 'type': 'string', 'defaultValue': 'charging.', 'description': 'To use correct credentitials and vCenter. Normally the product the host belongs to. Ex: "cil.", "charging.", "cpm.", "mapt", "ss7translator." . Use "charging." for UMIs vmy targethosts in VCenter05.<br />\n' +
            'Note: It should end with a dot!'],
        ['name': 'HOSTPROFILE', 'type': 'string', 'defaultValue': '', 'description': '<b>OPTIONAL.</b>Specifies machine specific properties: vmfolder, hypervisorname, datastorename and network. If "umi." is used it will end up in "UMI KA" folder, but you can also use your signum, ex "epdpdpd." and it will be in your folder in UMI KA.<br />\n' + 
            'Note: It should end with a dot!'],
        ['name': 'PRODUCT', 'type': 'string', 'defaultValue': 'charging', 'description': 'The product the TARGETHOST belongs to. normally the same as VMAPI_PREFIX (but WITHOUT the ending dot). Ex: "charging", "cil", "cpm", "ss7translator" .']
]
editableEmailNotification = [
        'recipient'            : 'eldin.malkoc@ericsson.com',
        'subject'              : 'Charging washingmachine on demand failed!',
        'content'              : 'Tapas session: $tapas_web_url',
        'failureTriggerSubject': '$PROJECT_DEFAULT_SUBJECT'
]
