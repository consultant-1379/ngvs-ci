params = [
        ['name'          : 'ACTION',
         'type'          : 'active_choice',
         'description'   : 'Choose action to enable or disable the EDM WashingMachine.',
         'choiceType'    : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'JobEnabledScriptBuilder',
         'scriptBuilderParams': [
                 projectName: 'edm_washingmachine'
         ],
         'fallbackScript': 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'  : false
        ],
        ['name': 'REASON', 'type': 'string', 'defaultValue': '', 'description': 'The reason why you are changing state of the EDM WashineMachine.']
]
editableEmailNotification = [
        'recipient'           : 'edm_washingmachine@mailman.lmera.ericsson.se',
        'subject'             : 'EDM WashingMachine state changed to $ACTION',
        'content'             : 'EDM WashingMachine state changed to $ACTION by $BUILD_USER_ID with reason \'$REASON\'',
        'alwaysTriggerSubject': '$PROJECT_DEFAULT_SUBJECT'
]
