package com.ericsson.bss.job.chargingaccess

import com.ericsson.bss.job.GerritPomCheckJobBuilder

class ChargingAccessGerritPomCheckJobBuilder extends GerritPomCheckJobBuilder {
    @Override
    protected void setPublishers() {
        job.with {
            publishers {
                archiveArtifacts('console_out.txt')
                textFinder('Unused property', 'console_out.txt', false, false, true)
                flexiblePublish {
                    conditionalAction {
                        condition { alwaysRun() }
                        steps {
                            shell('cat console_out.txt')
                        }
                    }
                }
            }
        }
        //super.setPublishers();
    }
}
