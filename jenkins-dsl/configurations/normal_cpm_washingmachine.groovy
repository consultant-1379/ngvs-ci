projectDescription = '<h1>Jive test</h1>\n' +
        '<a href="https://jive.epk.ericsson.se/#/projects/cpm/session-groups?session-groups-filter=%7B%22user.name%22:%22kascmadm%22%7D">Jive test logs</a>\n' +
        '<h1>Historical performance indication</h1>\n' +
        '<a href="http://internal.eta.epk.ericsson.se/proj/kacx/artifacts/rrdtool/cpm/performance/history-throughput.png"><img\n' +
        '  src="http://internal.eta.epk.ericsson.se/proj/kacx/artifacts/rrdtool/cpm/performance/history-throughput.png" width="400" height="200"></a>\n' +
        '<a href="http://internal.eta.epk.ericsson.se/proj/kacx/artifacts/rrdtool/cpm/performance/history-avg-response.png"><img\n' +
        '  src="http://internal.eta.epk.ericsson.se/proj/kacx/artifacts/rrdtool/cpm/performance/history-avg-response.png" width="400" height="200"></a>\n' +
        '<h1>CPM business flow - 5 minute performance graph from last execution</h1>\n' +
        '<a href="http://internal.eta.epk.ericsson.se/proj/kacx/artifacts/rrdtool/cpm/performance/latest2-throughput.png"><img\n' +
        '  src="http://internal.eta.epk.ericsson.se/proj/kacx/artifacts/rrdtool/cpm/performance/latest2-throughput.png" width="400" height="200"></a>\n' +
        '<a href="http://internal.eta.epk.ericsson.se/proj/kacx/artifacts/rrdtool/cpm/performance/latest2-avg-response.png"><img\n' +
        '  src="http://internal.eta.epk.ericsson.se/proj/kacx/artifacts/rrdtool/cpm/performance/latest2-avg-response.png" width="400" height="200"></a>\n' +
        '<a href="http://internal.eta.epk.ericsson.se/proj/kacx/artifacts/rrdtool/cpm/performance/latest2-vm-cpu1.png"><img\n' +
        '  src="http://internal.eta.epk.ericsson.se/proj/kacx/artifacts/rrdtool/cpm/performance/latest2-vm-cpu1.png" width="400" height="200"></a>\n' +
        '<a href="http://internal.eta.epk.ericsson.se/proj/kacx/artifacts/rrdtool/cpm/performance/latest2-vm-mem.png"><img\n' +
        '  src="http://internal.eta.epk.ericsson.se/proj/kacx/artifacts/rrdtool/cpm/performance/latest2-vm-mem.png" width="400" height="200"></a>\n' +
        '<h1>JMeter</h1>\n' +
        '<a href="http://internal.eta.epk.ericsson.se/proj/kacx/artifacts/rrdtool/cpm/performance/jmeter/result/report/index.html">JMeter interactive graphs</a>\n'

projectsToTrigger = [
    'projects': ['cpm_washingmachine_blame'],
    'parameters': [
        [
            'type' : 'predefinedProp',
            'name' : 'JENKINS_URL',
            'value' : '$BUILD_URL'
        ],
        [
            'type' : 'predefinedProp',
            'name' : 'TAPAS_URL',
            'value' : '$tapas_web_url'
        ],
        [
            'type' : 'predefinedProp',
            'name' : 'JIVE_URL',
            'value' : '$jive_web_url'
        ],
        [
            'type' : 'predefinedProp',
            'name' : 'UPSTREAM_JOB',
            'value' : '$JOB_NAME'
        ],
        [
            'type' : 'predefinedProp',
            'name' : 'BLAME_CONFIG_FILE',
            'value' : ('/proj/eta-automation/tapas/config/cpm/config/blame/general-blame-config.json,' +
                       '/proj/eta-automation/blame_mail/wm-blame-config-cpm.json')
        ],
        [
            'type' : 'predefinedProp',
            'name' : 'STATUS',
            'value' : '$WM_BLAME_STATUS'
        ],
        [
            'type' : 'predefinedProp',
            'name' : 'CISCAT_RESULT',
            'value' : '$CISCAT_RESULT'
        ],
        [
            'type' : 'predefinedProp',
            'name' : 'DEFAULT_RECIPIENTS',
            'value' : 'cpm_washingmachine@mailman.lmera.ericsson.se'
        ]
    ]
]

