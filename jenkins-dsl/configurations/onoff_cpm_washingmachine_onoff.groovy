params = [
        ['name'          : 'ACTION',
         'type'          : 'active_choice',
         'description'   : 'Choose action to enable or disable the CPM WashingMachine.',
         'choiceType'    : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'JobEnabledScriptBuilder',
         'scriptBuilderParams': [
                 projectName: 'cpm_washingmachine'
         ],
         'fallbackScript': 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'  : false
        ],
        ['name': 'REASON', 'type': 'string', 'defaultValue': '', 'description': 'The reason why you are changing state of the CPM WashineMachine.']
]
editableEmailNotification = [
        'recipient'           : 'cpm_washingmachine@mailman.lmera.ericsson.se',
        'subject'             : 'CPM WashingMachine state changed to $ACTION',
        'content'             : 'CPM WashingMachine state changed to $ACTION by $BUILD_USER_ID with reason \'$REASON\'',
        'alwaysTriggerSubject': '$PROJECT_DEFAULT_SUBJECT'
]
