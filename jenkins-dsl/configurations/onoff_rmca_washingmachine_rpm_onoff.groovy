projectDescription = 'A job to enable and disable RMCA WashingMachine RPM to allow troubleshooting on targethosts.'
params = [
        ['name'               : 'ENABLED_HOSTS',
         'type'               : 'active_choice',
         'description'        : 'Choose to use alternate clusters, a specifc cluster or none to disable.<br/>\n' +
                                '1: RMCA=vma-rmca0007, MSV=vma-rmca0008, CIL=vma-rmca0009<br/>\n',
         'choiceType'         : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'EnabledHostsScriptBuilder',
         'scriptBuilderParams': [
                 projectName: 'rmca_washingmachine_rpm'
         ],
         'fallbackScript': 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'  : false
        ],
        ['name': 'REASON', 'type': 'string', 'defaultValue': '', 'description': 'The reason why you are changing state of RMCA WashineMachine RPM.']
]
editableEmailNotification = [
        'recipient'           : 'rmca_washingmachine_rpm@mailman.lmera.ericsson.se',
        'subject'             : 'RMCA WashingMachine RPM state changed to $ACTION',
        'content'             : 'RMCA WashingMachine RPM state changed to $ACTION by $BUILD_USER_ID with reason \'$REASON\'',
        'alwaysTriggerSubject': '$PROJECT_DEFAULT_SUBJECT'
]
