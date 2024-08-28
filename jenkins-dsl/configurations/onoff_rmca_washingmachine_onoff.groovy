projectDescription = 'A job to enable and disable RMCA WashingMachine to allow troubleshooting on targethosts.'
params = [
        ['name'               : 'ENABLED_HOSTS',
         'type'               : 'active_choice',
         'description'        : 'Choose to use alternate clusters, a specifc cluster or none to disable.<br/>\n' +
                                '1: RMCA=vma-rmca0003, MSV=vma-rmca0001, CIL=vma-rmca0002<br/>\n' +
                                '2: RMCA=vma-rmca0006, MSV=vma-rmca0004, CIL=vma-rmca0005<br/>\n',

         'choiceType'         : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'EnabledHostsScriptBuilder',
         'scriptBuilderParams': [
                 projectName: 'rmca_washingmachine'
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'       : false
        ],
        ['name': 'REASON', 'type': 'string', 'defaultValue': '', 'description': 'The reason why you are changing state of RMCA WashineMachine.']
]
editableEmailNotification = [
        'recipient'           : 'rmca_washingmachine@mailman.lmera.ericsson.se',
        'subject'             : 'RMCA WashingMachine state changed to $ENABLED_HOSTS',
        'content'             : 'RMCA WashingMachine state changed to $ENABLED_HOSTS by $BUILD_USER_ID with reason \'$REASON\'',
        'alwaysTriggerSubject': '$PROJECT_DEFAULT_SUBJECT'
]
