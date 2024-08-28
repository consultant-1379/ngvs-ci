package com.ericsson.bss.job.cil

import com.ericsson.bss.job.MvnGerritUnitTestJobBuilder

class CilGerritUnitTestJobBuilder extends MvnGerritUnitTestJobBuilder {

    @Override
    protected void setPublishers() {
        super.setPublishers()
        reportViolations()
    }

    // report PMD violations due to CIL introduced maven pmd plugin
    protected void reportViolations(){
        job.with{
            publishers {
                violations(50) {
                    perFileDisplayLimit(51)
                    pmd(10, 999, 999, '**/pmd.xml')
                }
            }
        }
    }
}
