projectDescription = 'A job to enable and disable Charging WashingMachine RPM to allow troubleshooting on targethosts.'
params = [
        ['name'               : 'ENABLED_HOSTS',
         'type'               : 'active_choice',
         'description'        : 'Choose to use alternate clusters, a specifc cluster or none. Use none is disable ',
         'choiceType'         : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'EnabledHostsScriptBuilder',
         'scriptBuilderParams': [
                 projectName: 'charging_washingmachine_rpm'
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'       : false
        ],
        ['name': 'REASON', 'type': 'string', 'defaultValue': '', 'description': 'The reason why you are changing state of Charging WashineMachine.'],
        ['name'               : 'CORERPMVERSION',
         'type'               : 'active_choice_reactive_reference_param',
         'description'        : 'Core server rpm version to use. Default is LATEST',
         'choiceType'         : 'FORMATTED_HTML',
         'scriptBuilderName'  : 'SelectedStringScriptBuilder',
         'scriptBuilderParams': [
                 projectName  : 'charging_washingmachine_rpm',
                 parameterName: 'CORERPMVERSION',
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.", ""]',
         'referencedParameters': ['']
        ],
        ['name'               : 'ACCESSRPMVERSION',
         'type'               : 'active_choice_reactive_reference_param',
         'description'        : 'Access server rpm version to use. Default is LATEST',
         'choiceType'         : 'FORMATTED_HTML',
         'scriptBuilderName'  : 'SelectedStringScriptBuilder',
         'scriptBuilderParams': [
                 projectName  : 'charging_washingmachine_rpm',
                 parameterName: 'ACCESSRPMVERSION',
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.", ""]',
         'referencedParameters': ['']
        ],
        ['name'               : 'SERVICERPMVERSION',
         'type'               : 'active_choice_reactive_reference_param',
         'description'        : 'Servicelogics rpm version to use. Default is LATEST',
         'choiceType'         : 'FORMATTED_HTML',
         'scriptBuilderName'  : 'SelectedStringScriptBuilder',
         'scriptBuilderParams': [
                 projectName  : 'charging_washingmachine_rpm',
                 parameterName: 'SERVICERPMVERSION',
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.", ""]',
         'referencedParameters': ['']
        ],
        ['name'               : 'DLBRPMVERSION',
         'type'               : 'active_choice_reactive_reference_param',
         'description'        : 'Dlb rpm version to use. Default is LATEST',
         'choiceType'         : 'FORMATTED_HTML',
         'scriptBuilderName'  : 'SelectedStringScriptBuilder',
         'scriptBuilderParams': [
                 projectName  : 'charging_washingmachine_rpm',
                 parameterName: 'DLBRPMVERSION',
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.", ""]',
         'referencedParameters': ['']
        ],
        ['name'               : 'JIVEVERSION',
         'type'               : 'active_choice_reactive_reference_param',
         'description'        : 'Jive Version to use in JiveTest. Default is LATEST',
         'choiceType'         : 'FORMATTED_HTML',
         'scriptBuilderName'  : 'SelectedStringScriptBuilder',
         'scriptBuilderParams': [
                 projectName  : 'charging_washingmachine_rpm',
                 parameterName: 'JIVEVERSION',
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.", ""]',
         'referencedParameters': ['']
        ]
]
editableEmailNotification = [
        'recipient'           : 'charging_washingmachine_rpm@mailman.lmera.ericsson.se',
        'subject'             : 'Charging WashingMachine RPM state changed to $ENABLED_HOSTS',
        'content'             : 'Charging WashingMachine RPM state changed to $ENABLED_HOSTS by $BUILD_USER_ID with reason \'$REASON\'\n' +
                'Core RPM Version: $CORERPMVERSION\n' +
                'Access RPM Version: $ACCESSRPMVERSION\n' +
                'Access Service logics RPM Version: $SERVICERPMVERSION\n' +
                'DLB RPM Version: $DLBRPMVERSION\n' +
                'Jive Version: $JIVEVERSION',
        'alwaysTriggerSubject': '$PROJECT_DEFAULT_SUBJECT'
]