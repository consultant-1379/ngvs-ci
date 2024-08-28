package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.GerritSonarJobBuilder
import com.ericsson.bss.job.SonarJobBuilder
import com.ericsson.bss.job.eta.JenkinsDSLGerritSonarJobBuilder
import com.ericsson.bss.job.eta.JenkinsDSLSonarJobBuilder

class JenkinsDSL extends Project {

    public static final String PROJECT_NAME = "jenkins-dsl"
    public static final String RESTRICT_LABEL_KASCMADM = "kascmadm_Linux_Ubuntu_14.04_x86_64"

    public JenkinsDSL() {
        super.projectName = PROJECT_NAME
        super.mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-eta.xml"
    }

    @Override
    public void create(parent) {
        this.init(parent)
        this.createViews()
        this.createWashingMachinesJobs()

        def repositories = this.getRepositories()
        repositories.each {
            String repository = it
            gerritName = repository
            jobName = getJobName(repository)
            this.createForRepository()
        }
        createDslJob(repositories)
    }

    @Override
    protected List getRepositories() {
        List<String> etaRepositories = new ArrayList()
        etaRepositories.add("tools/eta/jenkins-dsl")
        return etaRepositories
    }

    @Override
    protected void createForRepository() {
        out.println("projectName: " + projectName + ", jobname: " + jobName + ", gerritName: " + gerritName)
        createFolders()
        createSonar()
        createSonarGerrit()
    }

    @Override
    protected void createFolders() {
        out.println("createFolders()")
        folderName = jobName
        dslFactory.folder(folderName) {}
        jobName = jobName.replace('.', '_')
    }

    @Override
    protected void createSonarGerrit() {
        out.println("createSonarGerrit()")
        GerritSonarJobBuilder gerritSonarJobBuilder = new JenkinsDSLGerritSonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_gerrit_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                projectName: projectName,
                verboseGerritFeedback: verboseGerritFeedback,
                dslFactory: dslFactory,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName)
        )

        gerritSonarJobBuilder.build()
    }

    @Override
    protected void createSonar() {
        out.println("createSonar()")
        SonarJobBuilder sonarJobBuilder = new JenkinsDSLSonarJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                jobName: folderName + "/" + jobName + "_sonar",
                mavenRepositoryPath: mavenRepositoryPath,
                mavenSettingsFile: mvnSettingFile,
                gerritName: gerritName,
                dslFactory: dslFactory,
                generateGUIconfig: shouldRepositoryHaveGuiConfig(jobName)
        )

        sonarJobBuilder.build()
    }

    @Override
    protected String getJobName(String repositoryName) {
        return repositoryName.substring(repositoryName.lastIndexOf(delimiter) + 1)
    }

    @Override
    protected void createViews() {
        dslFactory.nestedView("Jenkins DSL") {
            configure { project ->
                project / defaultView << 'All'
            }
            views{
                listView('All') {
                    jobs { regex(projectName + '.*') }
                    columns {
                        status()
                        weather()
                        name()
                        lastDuration()
                        lastBuildConsole()
                        lastSuccess()
                        lastFailure()
                        configureProject()
                        buildButton()
                    }
                }
            }
            views{
                listView('Sonar') {
                    jobs { regex(projectName + '/.*_sonar') }
                    columns {
                        status()
                        weather()
                        name()
                        lastDuration()
                        lastBuildConsole()
                        lastSuccess()
                        lastFailure()
                        configureProject()
                        buildButton()
                    }
                    recurse(true)
                }
            }
        }
    }
}
