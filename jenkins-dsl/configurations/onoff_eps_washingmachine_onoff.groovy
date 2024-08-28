params = [
        ['name'          : 'ACTION',
         'type'          : 'active_choice',
         'description'   : 'Choose action to enable or disable the EPS WashingMachine.',
         'choiceType'    : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'JobEnabledScriptBuilder',
         'scriptBuilderParams': [
                 projectName: 'eps_washingmachine'
         ],
         'fallbackScript': 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'  : false
        ],
        ['name': 'REASON', 'type': 'string', 'defaultValue': '', 'description': 'The reason why you are changing state of the EPS WashineMachine.']
]
editableEmailNotification = [
        'recipient'           : 'eps_washingmachine@mailman.lmera.ericsson.se',
        'subject'             : 'EPS WashingMachine state changed to $ACTION',
        'content'             : 'EPS WashingMachine state changed to $ACTION by $BUILD_USER_ID with reason \'$REASON\'',
        'alwaysTriggerSubject': '$PROJECT_DEFAULT_SUBJECT'
]
