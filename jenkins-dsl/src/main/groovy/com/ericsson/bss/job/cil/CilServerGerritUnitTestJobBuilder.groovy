package com.ericsson.bss.job.cil

class CilServerGerritUnitTestJobBuilder extends CilGerritUnitTestJobBuilder {
    @Override
    protected void extraShellSteps() {
        job.with {
            steps {
                shell(removeOldArtifacts())
                shell(setCilServerVirtualenvDir())
                shell(mavenBuildCommand())
                shell(junitPublisherWorkaround())
            }
        }
    }
}
