projectDescription = 'Continuous regression testing of the RMCA product.\n' +
        '\n' +
        'More information about the team clusters can be found at the following link:<br>\n' +
        '<a href="https://rmca.epk.ericsson.se/wiki/index.php/Lab_Hardware">Lab Hardware</a>'
common_env_variables = [
        'MAVEN_SETTINGS'    : '/proj/eta-automation/maven/kascmadm-settings_arm-rmca.xml'
]
params = [
        ['name'          : 'CLUSTER',
         'type'          : 'active_choice',
         'description'   : 'Target Cluster',
         'choiceType'    : 'SINGLE_SELECT',
         'scriptUrl'     : 'scripts/washingmachine/rmca_ondemand_CLUSTER_param.groovy',
         'fallbackScript': 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'  : false
        ],
        ['name'                : 'TARGETHOST',
         'type'                : 'active_choice_reactive_reference_param',
         'description'         : 'RMCA host',
         'choiceType'          : 'FORMATTED_HTML',
         'scriptUrl'           : 'scripts/washingmachine/rmca_ondemand_TARGETHOST_param.groovy',
         'fallbackScript'      : 'return ["Error evaluating Groovy script.", ""]',
         'referencedParameters': ['CLUSTER']
        ],
        ['name'                : 'CIL',
         'type'                : 'active_choice_reactive_reference_param',
         'description'         : 'Cil host',
         'choiceType'          : 'FORMATTED_HTML',
         'scriptUrl'           : 'scripts/washingmachine/rmca_ondemand_CIL_param.groovy',
         'fallbackScript'      : 'return ["Error evaluating Groovy script.", ""]',
         'referencedParameters': ['CLUSTER']
        ],
        ['name'                : 'MSV',
         'type'                : 'active_choice_reactive_reference_param',
         'description'         : 'Msv',
         'choiceType'          : 'FORMATTED_HTML',
         'scriptUrl'           : 'scripts/washingmachine/rmca_ondemand_MSV_param.groovy',
         'fallbackScript'      : 'return ["Error evaluating Groovy script.", ""]',
         'referencedParameters': ['CLUSTER']
        ],
        ['name'                : 'INSTALLNODE',
         'type'                : 'active_choice_reactive_reference_param',
         'description'         : '<b>[Mandatory]</b> Installnode',
         'choiceType'          : 'FORMATTED_HTML',
         'scriptUrl'           : 'scripts/washingmachine/rmca_ondemand_INSTALLNODE_param.groovy',
         'fallbackScript'      : 'return ["Error evaluating Groovy script.", ""]',
         'referencedParameters': ['CLUSTER']
        ],
        ['name'          : 'RMCASTAGINGVERSION',
         'type'          : 'active_choice',
         'description'   : 'Version of the RMCA staging directory artifact. Use LATEST to get the latest SNAPSHOT version.',
         'choiceType'    : 'SINGLE_SELECT',
         'scriptUrl'     : 'scripts/washingmachine/rmca_ondemand_RMCASTAGINGVERSION_param.groovy',
         'fallbackScript': 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'  : false
        ],
        ['name'          : 'JIVEVERSION',
         'type'          : 'active_choice',
         'description'   : 'Version of Jive test artifact.',
         'choiceType'    : 'SINGLE_SELECT',
         'scriptUrl'     : 'scripts/washingmachine/rmca_ondemand_RMCASTAGINGVERSION_param.groovy',
         'fallbackScript': 'return ["Error evaluating Groovy script.", ""]',
         'isFilterable'  : false
        ],
        ['name': 'RMCARPM', 'type': 'string', 'defaultValue': '', 'description': '<b>[Optional]</b> Specifies a custom built RMCA rpm to use. The file must be accessable from Jenkins as a filepath (ie /workarea/.. or /proj/..) or a URL.']
]
editableEmailNotification = [
        'recipient'             : 'eldin.malkoc@ericsson.com',
        'subject'               : 'RMCA washingmachine on demand!',
        'content'               : 'Tapas session: $tapas_web_url',
        'failureTriggerSubject' : 'RMCA washingmachine on demand Failed!',
        'fixedTriggerSubject'   : 'RMCA washingmachine on demand Success!',
        'unstableTriggerSubject': 'RMCA washingmachine on demand Unstable!',
        'abortedTriggerSubject' : 'RMCA washingmachine on demand aborted or timeout!'
]
