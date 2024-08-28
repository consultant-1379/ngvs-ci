params = [
        ['name'          : 'ACTION',
         'type'          : 'active_choice',
         'description'   : 'Choose action to enable or disable the ERMS WashingMachine.',
         'choiceType'    : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'JobEnabledScriptBuilder',
         'scriptBuilderParams': [
                 projectName: 'erms_washingmachine'
         ],
         'fallbackScript': 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'  : false
        ],
        ['name': 'REASON', 'type': 'string', 'defaultValue': '', 'description': 'The reason why you are changing state of the ERMS WashineMachine.']
]
editableEmailNotification = [
        'recipient'           : 'erms_washingmachine@mailman.lmera.ericsson.se',
        'subject'             : 'ERMS WashingMachine state changed to $ACTION',
        'content'             : 'ERMS WashingMachine state changed to $ACTION by $BUILD_USER_ID with reason \'$REASON\'',
        'alwaysTriggerSubject': '$PROJECT_DEFAULT_SUBJECT'
]
