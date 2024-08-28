params = [
        ['name'          : 'ACTION',
         'type'          : 'active_choice',
         'description'   : 'Choose action to enable or disable the Finance WashingMachine.',
         'choiceType'    : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'JobEnabledScriptBuilder',
         'scriptBuilderParams': [
                 projectName: 'finance_washingmachine'
         ],
         'fallbackScript': 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'  : false
        ],
        ['name': 'REASON', 'type': 'string', 'defaultValue': '', 'description': 'The reason why you are changing state of the Finance WashineMachine.']
]
editableEmailNotification = [
        'recipient'           : 'finance_washingmachine@mailman.lmera.ericsson.se',
        'subject'             : 'Finance WashingMachine state changed to $ACTION',
        'content'             : 'Finance WashingMachine state changed to $ACTION by $BUILD_USER_ID with reason \'$REASON\'',
        'alwaysTriggerSubject': '$PROJECT_DEFAULT_SUBJECT'
]
