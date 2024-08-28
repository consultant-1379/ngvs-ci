projectDescription = 'Installs latest CIL and runs EFTF tests.'
params = [
        ['name': 'TARGETHOST', 'type': 'string', 'defaultValue': '', 'description': 'The machine that should be installed.'],
        ['name': 'TARGETHOST2', 'type': 'string', 'defaultValue': '', 'description': 'The second targethost to be installed.'],
        ['name': 'TARGETHOST3', 'type': 'string', 'defaultValue': '', 'description': 'The third targethost to be installed.'],
        ['name': 'INSTALLNODE', 'type': 'string', 'defaultValue': '', 'description': 'The InstallNode which performs the install and creates the OVF.'],
        ['name': 'MSV', 'type': 'string', 'defaultValue': '', 'description': 'The MSV that performs the OVF deploy and also will store info about targethost in zookeeper.']
]
gitRepoUrl = 'ssh://gerrit.epk.ericsson.se:29418/eftf/cil'
emailNotification = [
        'recipients': ['patrick.a.ziegler@ericsson.com', 'marius.paun@ericsson.com']
]
