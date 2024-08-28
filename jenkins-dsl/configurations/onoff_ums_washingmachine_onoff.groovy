projectDescription = 'A job to enable and disable UMS WashingMachine to allow troubleshooting on targethosts.'
params = [
        ['name'               : 'ACTION',
         'type'               : 'active_choice',
         'description'        : 'Choose action to enable or disable the UMS WashingMachine.',
         'choiceType'         : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'JobEnabledScriptBuilder',
         'scriptBuilderParams': [
                 projectName: 'ums_washingmachine'
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'       : false
        ],
        ['name': 'REASON', 'type': 'string', 'defaultValue': '', 'description': 'The reason why you are changing state of UMS WashineMachine.']
]
editableEmailNotification = [
        'recipient'           : 'ums_washingmachine@mailman.lmera.ericsson.se',
        'subject'             : 'UMS WashingMachine state changed to $ACTION',
        'content'             : 'UMS WashingMachine state changed to $ACTION by $BUILD_USER_ID with reason \'$REASON\'',
        'alwaysTriggerSubject': '$PROJECT_DEFAULT_SUBJECT'
]
