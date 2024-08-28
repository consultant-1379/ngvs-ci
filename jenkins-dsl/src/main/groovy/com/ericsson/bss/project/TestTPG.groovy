package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.MultiRepositoryReleaseJobBuilder
import com.ericsson.bss.util.GitUtil

class TestTPG extends Project {
    private List repositoriesToRelease
    public String projectName = "testtpg"

    public TestTPG(){
        super.projectName = this.projectName
    }

    @Override
    public void init(parent) {
        super.init(parent)

        mvnSettingFile = mvnSettingFilePath + "kascmadm-settings_arm-testtpg.xml"

        repositoriesToRelease = []
        repositoriesToRelease.add('ssh://kagitolite/test/release.repo/a')
        repositoriesToRelease.add('ssh://kagitolite/test/release.repo/bundle')
        repositoriesToRelease.add('ssh://kagitolite/test/release.repo/integration')
        repositoriesToRelease.add('ssh://kagitolite/test/release.repo/b')
        repositoriesToRelease.add('ssh://kagitolite/test/release.repo/productiondependencies')
        repositoriesToRelease.add('ssh://kagitolite/test/release.repo/top')
    }

    @Override
    protected List getRepositories() {
        List<String> repositories = new ArrayList()

        repositories.add("tools/eta/testtpg")
        repositories.addAll(repositoriesToRelease)
        repositories.add("kagitolite:test/testtpg/ui")
        repositories.add("kagitolite:test/testtpg/uitestproject")

        out.println("repositories: " + repositories)

        return repositories
    }

    @Override
    protected List getGuiRepositories() {
        List<String> repositories = []
        repositories.add('uitestproject')

        return repositories
    }

    @Override
    protected List getNpmRepositories() {
        return ["kagitolite:test/testtpg/ui"]
    }

    @Override
    protected String getJobName(String repository) {
        if (GitUtil.isLocatedInGitolite(repository)) {
            return repository[(repository.lastIndexOf('/') + 1)..-1]
        }
        else {
            return super.getJobName(repository).replace('eta/', '')
        }
    }

    @Override
    public void create(parent) {
        super.create(parent)
        createMultiRepositoryReleaseJob()
    }

    private void createMultiRepositoryReleaseJob() {
        out.println("createReleaseJob()")

        MultiRepositoryReleaseJobBuilder multiRepositoryReleaseJobBuilder = new MultiRepositoryReleaseJobBuilder(
                gerritUser: gerritUser,
                gerritServer: gerritServer,
                projectName: projectName,
                mavenRepositoryPath: mavenRepositoryPath,
                repositoryList: repositoriesToRelease,
                buildName: 'testtpg-release',
                releaseRepository:'proj-testtpg-release-local',
                stagingRepository: 'proj-testtpg-staging-local',
                mavenSettingsFile: mvnSettingFile,
                useReleaseVersion: false,
                dslFactory: dslFactory
                )

        multiRepositoryReleaseJobBuilder.build()
    }
}
