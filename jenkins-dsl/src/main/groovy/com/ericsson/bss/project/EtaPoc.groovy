package com.ericsson.bss.project

import com.ericsson.bss.Project
import com.ericsson.bss.job.etapoc.GerritCodeFreezeJobBuilder
import com.ericsson.bss.util.GerritUtil

class EtaPoc extends Project {

    public static final String PROJECT_NAME = "eta.poc"
    public static final String REPOSITORY_PREFIX = "ejohadu_"

    public EtaPoc() {
        super.projectName = PROJECT_NAME
    }

    @Override
    protected List getRepositories() {
        String output = executeCommand(GerritUtil.getAllProjectsCommand(gerritServer))
        List<String> repositories = []
        for (repository in output.split(System.lineSeparator())) {
            if (repository.contains(REPOSITORY_PREFIX)) {
                repositories.add(repository)
            }
        }

        return repositories
    }

    @Override
    public void create(parent) {
        super.init(parent)
        createGerritCodeFreezeJob()
    }

    @Override
    protected void createGerritCodeFreezeJob() {
        out.println("createGerritCodeFreezeJob()")
        GerritCodeFreezeJobBuilder createGerritCodeFreezeJobBuilder = new GerritCodeFreezeJobBuilder(
                gerritUser: GERRIT_EPK_USER,
                gerritServer: GERRIT_EPK_SERVER,
                gerritName: 'tools/etacommon',
                jobName: projectName + '_code_freeze',
                dslFactory: dslFactory,
                repositories: getRepositories()
        )

        createGerritCodeFreezeJobBuilder.build()
    }
}
