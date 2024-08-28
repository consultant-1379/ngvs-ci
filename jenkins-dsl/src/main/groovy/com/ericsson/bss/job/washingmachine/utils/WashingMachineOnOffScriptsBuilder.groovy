package com.ericsson.bss.job.washingmachine.utils

import javax.management.BadAttributeValueExpException

/**
 * Builder for creating scripts for onoff washingmachines jobs.
 * For local tests PARAMS_FILES_DIR should be changed to local directory, since files in
 * proj folder are owned by 'kascmadm' - leaving this as it is right now will lead to permissions
 * errors and jobs wont be generated.
 */
class WashingMachineOnOffScriptsBuilder {

    private static final PARAMS_FILES_DIR_ANCHOR = '<PARAMS_FILES_DIR>'
    private static final PARAMS_FILES_DIR = '/proj/eta-automation/jenkins/kascmadm/job_config'
    private static final String PROJECT_NAME_ANCHOR = '<PROJECT_TO_BUILD_NAME>'
    private static final List<String> PROJECTS_WHICH_REQUIRES_PARAMS = new ArrayList<>()

    static {
        PROJECTS_WHICH_REQUIRES_PARAMS.add('charging_washingmachine')
        PROJECTS_WHICH_REQUIRES_PARAMS.add('charging_washingmachine_rpm')
        PROJECTS_WHICH_REQUIRES_PARAMS.add('rmca_washingmachine')
        PROJECTS_WHICH_REQUIRES_PARAMS.add('rmca_washingmachine_rpm')
        PROJECTS_WHICH_REQUIRES_PARAMS.add('rmca_washingmachine_releasebranch')
        PROJECTS_WHICH_REQUIRES_PARAMS.add('rmca_washingmachine_releasebranch_2')
    }

    private def dslFactory
    private String projectToBuildName

    public static WashingMachineOnOffScriptsBuilder newBuilder(def dslFactory, String projectToBuildName) {
        return new WashingMachineOnOffScriptsBuilder([dslFactory: dslFactory, projectToBuildName: projectToBuildName])
    }

    public String build() {
        if (!projectToBuildName) {
            throw new BadAttributeValueExpException("Project name cannot be null or empty")
        }

        if (ifProjectNeedParamsFromFile(projectToBuildName)) {
            return dslFactory.readFileFromWorkspace('scripts/washingmachine/' + projectToBuildName + '_onoff.groovy')
                    .replace(PARAMS_FILES_DIR_ANCHOR, getProjectParamsFile(projectToBuildName))
        } else {
            return dslFactory.readFileFromWorkspace('scripts/washingmachine/simple_washingmachine_onoff.groovy')
                    .replace(PROJECT_NAME_ANCHOR, projectToBuildName)
                    .replace(PARAMS_FILES_DIR_ANCHOR, getProjectParamsFile(projectToBuildName))
        }
    }

    /**
     * Returns absolute path to params file for given project. Creates file if not exists but
     * directories must exist - user needs to have permissions to write/read to location.
     * @param projectToBuild Project to build which requires params from params file.
     * @return Absolute Path to params file.
     */
    public static String getProjectParamsFile(String projectNameToBuild) {
        String fileName = PARAMS_FILES_DIR + '/' + projectNameToBuild + '_params.properties'
        File f = new File(fileName)
        if (!f.exists())
            f.createNewFile()
        return fileName
    }

    /**
     * Checks if {@code projectToBuildName} requires parameters which are passed using a param file by keepalive job.
     * @param projectToBuildName Project which is triggered by keepalive job.
     * @return Returns true if keepalive job trigger washingmachine job using parameters from the params file.
     */
    protected static ifProjectNeedParamsFromFile(String projectToBuildName) {
        return PROJECTS_WHICH_REQUIRES_PARAMS.contains(projectToBuildName)
    }
}
