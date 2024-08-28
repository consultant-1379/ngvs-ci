projectDescription = 'Runs EFTF tests'
params = [
        ['name': 'TARGETHOST', 'type': 'string', 'defaultValue': 'vmx-cil009', 'description': 'The machine that should be installed.'],
        ['name': 'TARGETHOST2', 'type': 'string', 'defaultValue': 'vmx-cil010', 'description': 'The second targethost to be installed.'],
        ['name': 'TARGETHOST3', 'type': 'string', 'defaultValue': 'vmx-cil011', 'description': 'The third targethost to be installed.'],
        ['name': 'INSTALLNODE', 'type': 'string', 'defaultValue': 'vmx-cil019', 'description': 'The InstallNode which performs the install and creates the OVF.'],
        ['name': 'MSV', 'type': 'string', 'defaultValue': 'vmx-cil018', 'description': 'The MSV that performs the OVF deploy and also will store info about targethost in zookeeper.']
]
gitRepoUrl = 'ssh://gerrit.epk.ericsson.se:29418/eftf/cil'
cronExpression = 'H 4 * * *'
emailNotification = [
        'recipients': ['cil_washingmachine@mailman.lmera.ericsson.se']
]
