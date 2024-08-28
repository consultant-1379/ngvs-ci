package com.ericsson.bss.decorators

import javaposse.jobdsl.dsl.Job

class SCMDecorator<T extends SCMDecorator> {

    protected Job job

    SCMDecorator(Job job) {
        this.job = job
    }

    /**
     * Adds a Git SCM source.
     * @param repoURL Repository URL
     * @param branchToBuild Specify the branches to examine for changes and to build.
     * @param cleanUpBeforeCheckout Clean up the workspace before every checkout by deleting all untracked files and directories, including those which are specified in .gitignore. Defaults to false.
     */
    T addGitRepository(String repoURL, String branchToBuild, boolean cleanUpBeforeCheckout) {
        job.with {
            scm {
                git {
                    remote {
                        url(repoURL)
                    }
                    clean(cleanUpBeforeCheckout)
                    branch(branchToBuild)
                }
            }
        }
        return this
    }
}
