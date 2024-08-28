package com.ericsson.bss.job

class GerritUnitTestCheckstyleJobBuilder extends MvnGerritUnitTestJobBuilder {

    @Override
    protected String mavenBuildCommand() {
        super.mavenBuildCommand().replace('install \\\n', 'install \\\ncheckstyle:check  \\\n')
    }

    @Override
    protected void setPublishers() {
        job.with {
            publishers {
                checkstyle('**/checkstyle-result.xml', {
                    healthLimits 0, 0
                    thresholds( unstableTotal: [all: 0] )
                })
            }
        }

        super.setPublishers()
    }
}
