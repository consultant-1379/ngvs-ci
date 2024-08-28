package com.ericsson.bss.util.scriptbuilders

import com.ericsson.bss.Project
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import java.nio.file.Paths

public class SonarReviewPosterScriptBuilder {

    private static final String TEMPLATE = "mvn com.ericsson.eta.tools:sonar-review-poster-maven-plugin:post \\\n" +
            "--fail-never \\\n" +
            "-Djsse.enableSNIExtension=false \\\n" +
            "-Dsrp.sonarReport=\"%s\" \\\n" +
            "-Dsrp.conf=\"%s\" \\\n" +
            "-Dsrp.changeId=\${GERRIT_CHANGE_ID} \\\n" +
            "-Dsrp.revisionId=\${GERRIT_PATCHSET_REVISION} \\\n" +
            "-Dsrp.buildUrl=\${BUILD_URL} \\\n" +
            "-Dsrp.sonarGoalDir=\"%s\" \\\n" +
            "-Dsrp.projectName=\${GERRIT_PROJECT} \\\n" +
            "-Dsrp.branchName=\${GERRIT_BRANCH} \\\n" +
            "--settings \${MAVEN_SETTINGS} \\\n" +
            "-Dmaven.repo.local=\${MAVEN_REPOSITORY} \\\n" +
            "--non-recursive --batch-mode >> console_out.txt"

    private static final String WORKSPACE = "\${WORKSPACE}"
    public static final String DEFAULT_RELATIVE_SONAR_REPORT_PATH = "target/sonar/sonar-report.json"
    public static final String DEFAULT_PROJECT_DIRECTORY = ""
    public static final String DEFAULT_GERRIT_SERVER = Project.GERRIT_EPK_SERVER
    public static final String DEFAULT_GERRIT_USER = "jenkins-code-quality"
    private static final String DEFAULT_GERRIT_CONNECTION_CONFIG =
            "/proj/eta-automation/config/kascmadm/sonar_connection.json"
    private static final String DEFAULT_GERRIT_CONNECTION_CONFIG_KASCMADM =
            "/proj/eta-automation/config/kascmadm/sonar_connection_kascmadm.json"
    private static final String DEFAULT_GERRITFORGE_CONNECTION_CONFIG =
            "/proj/eta-automation/config/kascmadm/sonar_connection_gerritforge.json"
    private static final String DEFAULT_GERRITCENTRAL_CONNECTION_CONFIG =
            "/proj/eta-automation/config/kascmadm/sonar_connection_gerritcentral.json"

    /**
     * Get proper Sonar Review Poster execution script.
     * @return Script to execute Sonar Review Poster
     */
    public static String getScript() {
        return getScript(DEFAULT_PROJECT_DIRECTORY, DEFAULT_RELATIVE_SONAR_REPORT_PATH,
                DEFAULT_GERRIT_SERVER, DEFAULT_GERRIT_USER)
    }

    /**
     * Get proper Sonar Review Poster execution script.
     * @param projectDirectory Path to direcotry where "mvn sonar:sonar" gets executed. Relative to workspace.
     * @return Script to execute Sonar Review Poster
     */
    public static String getScript(String projectDirectory) {
        return getScript(projectDirectory, DEFAULT_RELATIVE_SONAR_REPORT_PATH,
                DEFAULT_GERRIT_SERVER, DEFAULT_GERRIT_USER)
    }

    /**
     * Get proper Sonar Review Poster execution script.
     * @param projectDirectory Path to direcotry where "mvn sonar:sonar" gets executed. Relative to workspace.
     * @param reportPath Relative path to sonar-report.json
     * @return Script to execute Sonar Review Poster
     */
    public static String getScript(String projectDirectory, String reportPath) {
        return getScript(projectDirectory, reportPath, DEFAULT_GERRIT_SERVER, DEFAULT_GERRIT_USER)
    }

    /**
     * Get proper Sonar Review Poster execution script.
     * @param projectDirectory Path to direcotry where "mvn sonar:sonar" gets executed. Relative to workspace.
     * @param reportPath Relative path to sonar-report.json
     * @param gerritServer Type of Gerrit server: EPK or FORGE
     * @return Script to execute Sonar Review Poster
     */
    public static String getScript(String projectDirectory, String reportPath, String gerritServer) {
        return getScript(projectDirectory, reportPath, gerritServer, DEFAULT_GERRIT_USER)
    }

    /**
     * Get proper Sonar Review Poster execution script.
     * @param projectDirectory Path to direcotry where "mvn sonar:sonar" gets executed. Relative to workspace.
     * @param reportPath Relative path to sonar-report.json
     * @param gerritServer Type of Gerrit server: EPK or FORGE
     * @param gerritUser Gerrit username to post reviews
     * @return Script to execute Sonar Review Poster
     */
    public static String getScript(String projectDirectory, String reportPath, String gerritServer, String gerritUser) {
        String sonarReport = Paths.get(WORKSPACE, projectDirectory, reportPath)
        String configFile = getConfigForGerrit(gerritServer, gerritUser)
        return String.format(
                TEMPLATE,
                sonarReport,
                configFile,
                projectDirectory)
    }

    private static String getConfigForGerrit(String gerritServer, String gerritUser) {
        if (gerritServer?.equals(Project.GERRIT_CENTRAL_SERVER)) {
            return DEFAULT_GERRITCENTRAL_CONNECTION_CONFIG
        }

        if (gerritServer?.equals(Project.GERRIT_FORGE_SERVER)) {
            if (gerritUser?.equals(Project.GERRIT_EPK_USER)) {
                throw new NotImplementedException()
            } else {
                return DEFAULT_GERRITFORGE_CONNECTION_CONFIG
            }
        } else {
            if (gerritUser?.equals(Project.GERRIT_EPK_USER)) {
                return DEFAULT_GERRIT_CONNECTION_CONFIG_KASCMADM
            } else {
                return DEFAULT_GERRIT_CONNECTION_CONFIG
            }
        }
    }
}
