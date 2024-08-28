package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.washingmachine.CivWashingMachineOnDemandJobBuilder

class Civ extends Project {
    public static String projectName = "civ"

    public Civ() {
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)

        gerritServer = GERRIT_CENTRAL_SERVER
        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-civ.xml"
    }

    @Override
    public void create(parent) {
        super.create(parent)
        createCivWashingMachineOnDemand()
    }

    @Override
    protected List getRepositories() {
        List repositories = []

        repositories.add("bssf/civ/com.ericsson.bss.rm.civ.core")

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected String getJobName(String repository) {
        return repository.replace('bssf/civ/com.ericsson.bss.rm.', '').replace('.', '_')
    }

    protected void createCivWashingMachineOnDemand() {
        out.println("createCivWashingMachineOnDemand()")

        CivWashingMachineOnDemandJobBuilder builder = CivWashingMachineOnDemandJobBuilder.newInstance(
                defaultTapasJobPath: 'CIV/CIV%20${TPG}%20Washingmachine',
                dslFactory: dslFactory,
                gerritServer: '',
                jobName: projectName + '_washingmachine_ondemand',
                out: out,
                projectName: projectName,
                suite: 'suites/washingmachine_ondemand.xml',
                suiteFile: projectName + '_washingmachine_ondemand_${TARGETHOST}',
                symlinkWorkspace: true,
                tapasProjectName: '${TPG,,}'
        )
        builder.build()
    }
}
