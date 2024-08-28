projectDescription = 'Runs EFTF tests'
params = [
        ['name': 'TARGETHOST', 'type': 'string', 'defaultValue': 'vmx-rminvoicing-055;vmx-rminvoicing-056;vmx-rminvoicing-057', 'description': 'If multiple machines, use ";" to separate them. Ex. vmx123;vmx456.'],
        ['name'               : 'VERSION',
         'type'               : 'active_choice',
         'description'        : 'Version of Invoicing Controller OVF to install. Use LATEST to get the latest working SNAPSHOT OVF from Washingmachine.',
         'choiceType'         : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'ProjectVersionsScriptBuilder',
         'scriptBuilderParams': [
                 url       : 'https://arm.epk.ericsson.se/artifactory/simple/proj-invoicing-release-local/com/ericsson/bss/INVCONTROLLER/maven-metadata.xml',
                 withLatest: true
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.","LATEST"]',
         'isFilterable'       : false
        ],
        ['name': 'TARGETHOST2', 'type': 'string', 'defaultValue': 'vmx-rminvoicing-058;vmx-rminvoicing-059;vmx-rminvoicing-060', 'description': 'If multiple machines, use ";" to separate them. Ex. vmx123;vmx456.'],
        ['name'               : 'VERSION2',
         'type'               : 'active_choice',
         'description'        : 'Version of Invoicing Processor OVF to install. Use LATEST to get the latest working SNAPSHOT OVF from Washingmachine.',
         'choiceType'         : 'SINGLE_SELECT',
         'scriptBuilderName'  : 'ProjectVersionsScriptBuilder',
         'scriptBuilderParams': [
                 url       : 'https://arm.epk.ericsson.se/artifactory/simple/proj-invoicing-release-local/com/ericsson/bss/INVPROCESSOR/maven-metadata.xml',
                 withLatest: true
         ],
         'fallbackScript'     : 'return ["Error evaluating Groovy script.","LATEST"]',
         'isFilterable'       : false
        ],
        ['name': 'CIL', 'type': 'string', 'defaultValue': 'vmx-rminvoicing-052;vmx-rminvoicing-053;vmx-rminvoicing-054', 'description': 'Host with LATEST CIL on it. If multiple machines, use ";" to separate them. Ex. vmx123;vmx456.'],
        ['name': 'MSV', 'type': 'string', 'defaultValue': 'vmx-rminvoicing-051', 'description': 'Host with LATEST MSV on it. ']
]

emailNotification = [
        'recipients': ['invoicing_washingmachine@mailman.lmera.ericsson.se']
]
