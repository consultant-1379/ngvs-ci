package com.ericsson.bss.job

import com.ericsson.bss.AbstractTapasJobBuilder
import com.ericsson.bss.util.Email

import javaposse.jobdsl.dsl.Job

public class TargethostInstallRpmJobBuilder extends AbstractTapasJobBuilder {

    protected String parameterName = "TARGETHOST"

    public Job build() {
        initProject(dslFactory.freeStyleJob(jobName))
        super.configurePostBuildSteps().editableEmailNotification(getAlwaysMailConfig())
        setDescription("<h2>Updates RPM for a " + projectName + " targethost.</h2>")
        addTimeoutAndAbortConfig(20)
        return job
    }

    @Override
    protected void setInputParameters() {
        addSelectClusterParameter('Choose cluster to update RPM on targethost')
        addClusterReferenceParameter(parameterName, 'Targethost which will updated with new RPM')
        addClusterReferenceParameter('CIL', 'The CIL for this cluster')
        job.with {
            parameters {
                stringParam('RPM', 'LATEST', 'The RPM to install. LATEST or a specific version ' +
                                             'will fetch from ARM. A file path accessible from ' +
                                             'Jenkins or a URL to a RPM will use the one ' +
                                             'specified.')
            }
        }
    }

    protected Email getAlwaysMailConfig()
    {
        return Email.newBuilder().withRecipient('$DEFAULT_RECIPIENTS')
                                 .withSubject('$DEFAULT_SUBJECT')
                                 .withContent('$DEFAULT_CONTENT')
                                 .withAlwaysTrigger()
                                 .build()
    }

    protected String getTapasParameters()
    {
        def params = '--define=__TARGETHOST__=${' + parameterName + '} \\\n' +
                '--define=__CIL__=\${CIL} \\\n' +
                '--define=__RPM__=\${RPM} \\\n'
        if (variant) {
            params += '--define=__VARIANT__=' + variant + ' \\\n'
        }
        return params
    }
}
