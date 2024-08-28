projectDescription = 'A job to enable and disable Charging WashingMachine to allow troubleshooting on targethosts.'
params = [
        ['name'               : 'ENABLED_HOSTS',
         'type'               : 'active_choice',
         'description'        : 'Choose to use alternate clusters, a specifc cluster or none. Use none is disable.<br/>\n' +
                                '1: CORE=vma-cha0008, ACCESS=vma-cha0009, DLB=vma-cha0010, CIL=vma-cha0007, MSV=vma-cha0006<br/>\n' +
                                '2: CORE=vma-cha0013, ACCESS=vma-cha0014, DLB=vma-cha0015, CIL=vma-cha0012, MSV=vma-cha0011',
         'choiceType'         : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'EnabledHostsScriptBuilder',
         'scriptBuilderParams': [
                 projectName: 'charging_washingmachine'
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'       : false
        ],

        ['name': 'REASON', 'type': 'string', 'defaultValue': '', 'description': 'The reason why you are changing state of Charging WashineMachine.'],

        ['name'               : 'COREOVFVERSION',
         'type'               : 'active_choice',
         'description'        : 'Version of Core OVF to install. Use LATEST to get the latest working SNAPSHOT OVF.',
         'choiceType'         : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'ProjectVersionsOnOffJobScriptBuilder',
         'scriptBuilderParams': [
                 url          : 'https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/ericsson/bss/CHACORE/maven-metadata.xml',
                 withLatest   : true,
                 projectName  : 'charging_washingmachine',
                 parameterName: 'COREOVFVERSION'
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'       : false
        ],

        ['name'               : 'ACCESSOVFVERSION',
         'type'               : 'active_choice',
         'description'        : 'Version of Access OVF to install. Use LATEST to get the latest working SNAPSHOT OVF.',
         'choiceType'         : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'ProjectVersionsOnOffJobScriptBuilder',
         'scriptBuilderParams': [
                 url          : 'https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/ericsson/bss/CHAACCESS/maven-metadata.xml',
                 withLatest   : true,
                 projectName  : 'charging_washingmachine',
                 parameterName: 'ACCESSOVFVERSION'
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'       : false
        ],

        ['name'               : 'DLBOVFVERSION',
         'type'               : 'active_choice',
         'description'        : 'Version of Dlb OVF to install. Use LATEST to get the latest working SNAPSHOT OVF.',
         'choiceType'         : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'ProjectVersionsScriptBuilder',
         'scriptBuilderParams': [
                 url       : 'https://arm.epk.ericsson.se/artifactory/proj-charging-release-local/com/ericsson/bss/CHADLB/maven-metadata.xml',
                 withLatest: true
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'       : false
        ],

        ['name'               : 'JIVEVERSION',
         'type'               : 'active_choice_reactive_reference_param',
         'description'        : 'LATEST for latest SNAPSHOT, or x.y.z / x.y.z-SNAPSHOT',
         'choiceType'         : 'FORMATTED_HTML',
         'scriptBuilderName'  : 'SelectedStringScriptBuilder',
         'scriptBuilderParams': [
                 projectName  : 'charging_washingmachine',
                 parameterName: 'JIVEVERSION',
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.", ""]',
         'referencedParameters': ['']
        ]
]
editableEmailNotification = [
        'recipient'           : 'charging_washingmachine@mailman.lmera.ericsson.se',
        'subject'             : 'Charging WashingMachine state changed to hosts $ENABLED_HOSTS',
        'content'             : 'Charging WashingMachine state changed to hosts $ENABLED_HOSTS by $BUILD_USER_ID with reason \'$REASON\'\n' +
                'Access Ovf Version: $ACCESSOVFVERSION\n' +
                'Core Ovf Version: $COREOVFVERSION\n' +
                'Dlb Ovf Version: $DLBOVFVERSION\n' +
                'Jive Version: $JIVEVERSION',
        'alwaysTriggerSubject': '$PROJECT_DEFAULT_SUBJECT'
]
