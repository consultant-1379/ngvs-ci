package com.ericsson.bss.project

import com.ericsson.bss.Project

class Msg extends Project {
    public static String projectName = "msg"

    public Msg() {
        super.projectName = this.projectName
    }

    public void create(parent) {
        this.init(parent)

        super.createTargethostInstallJob(
                versionLocation: 'https://arm.epk.ericsson.se/artifactory/proj-msgsys-release-local/com/ericsson/msg/broker/19010-CXP9025332/,' +
                        'https://arm.epk.ericsson.se/artifactory/proj-msgsys-release/com/ericsson/msg/msg-broker/;2.5.1/',
                defaultTapasJobPath: 'MSG%20Targethost%20Install',
                useDvFile: true,
                useCil: false)

    }

    @Override
    protected List getRepositories() {
        // Msg has its own automation for repositories (they use gradle)
        return []
    }
}
