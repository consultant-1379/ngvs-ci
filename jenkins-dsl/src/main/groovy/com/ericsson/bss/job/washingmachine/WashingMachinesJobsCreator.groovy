package com.ericsson.bss.job.washingmachine

import com.ericsson.bss.project.Charging
import com.ericsson.bss.project.Cil
import com.ericsson.bss.project.Coba
import com.ericsson.bss.project.Collection
import com.ericsson.bss.project.Cpi
import com.ericsson.bss.project.Erms
import com.ericsson.bss.project.Rmca
import com.ericsson.bss.project.Invoicing
import com.ericsson.bss.project.Edm
import com.ericsson.bss.project.Finance
import com.ericsson.bss.project.Mapt
import com.ericsson.bss.project.Num
import com.ericsson.bss.project.Eps
import com.ericsson.bss.project.Taxation
import com.ericsson.bss.project.Ums
import javaposse.jobdsl.dsl.DslFactory

class WashingMachinesJobsCreator {

    private static
    final String MIGRATION_CONF = "configurations/washingmachines_on_off_configuration.groovy"

    def out
    DslFactory dslFactory
    String workspacePath
    String gerritUser
    String gerritServer
    String projectName

    private migrationConfig

    public create() {
        readMigrationConfiguration()
        if (migrationConfig.enabled) {
            Map params = [workspacePath: workspacePath,
                          gerritUser   : gerritUser,
                          gerritServer : gerritServer,
                          dslFactory   : dslFactory,
                          projectName  : projectName,
                          out          : out]

            out.println("Creating washingmachines for " + projectName)
            switch (projectName) {
                case Charging.projectName:
                    if (migrationConfig.chargingEnabled) {
                        this.createChargingWashingMachines(params)
                    }
                    break
                case Coba.projectName:
                    if (migrationConfig.cobaEnabled) {
                        this.createCobaWashingMachines(params)
                    }
                    break
                case Collection.projectName:
                    if (migrationConfig.collectionEnabled) {
                        this.createCollectionWashingMachines(params)
                    }
                    break
                case Num.projectName:
                    if (migrationConfig.numEnabled) {
                        this.createNumWashingMachines(params)
                    }
                    break
                case Rmca.projectName:
                    if (migrationConfig.rmcaEnabled) {
                        this.createRmcaWashingMachines(params)
                    }
                    break
                case Cil.projectName:
                    if (migrationConfig.cilEnabled) {
                        this.createCilWashingMachines(params)
                    }
                    break
                case 'cus':
                    if (migrationConfig.cusEnabled) {
                        this.createCusWashingMachines(params)
                    }
                    break
                case Invoicing.projectName:
                    if (migrationConfig.invoicingEnabled) {
                        this.createInvoicingWashingMachines(params)
                    }
                    break
                case Edm.projectName:
                    if (migrationConfig.edmEnabled) {
                        this.createEdmWashingMachines(params)
                    }
                    break
                case Mapt.projectName:
                    if (migrationConfig.maptEnabled) {
                        this.createMaptWashingMachines(params)
                    }
                    break
                case Finance.projectName:
                    if (migrationConfig.financeEnabled) {
                        this.createFinanceWashingMachines(params)
                    }
                    break
                case Erms.projectName:
                    if (migrationConfig.ermsEnabled) {
                        this.createErmsWashingMachines(params)
                    }
                    break
                case Cpi.projectName:
                    if (migrationConfig.cpiEnabled) {
                        this.createCpiWashingMachines(params)
                    }
                    break
                case 'cpm':
                    if (migrationConfig.cpmEnabled) {
                        this.createCpmWashingMachines(params)
                    }
                    break
                case Eps.projectName:
                    if (migrationConfig.epsEnabled) {
                        this.createEpsWashingMachines(params)
                    }
                    break
                case Taxation.projectName:
                    if (migrationConfig.taxationEnabled) {
                        this.createTaxationWashingMachines(params)
                    }
                    break
                case Ums.projectName:
                    if (migrationConfig.umsEnabled) {
                        this.createUmsWashingMachines(params)
                    }
                    break
            }
        }
    }

    private createChargingWashingMachines(Map params) {
        if (migrationConfig.chargingWashingMachineEnabled) {
            new ChargingWashingMachineJobBuilder(params).build()
        }

        if (migrationConfig.chargingWashingMachineKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(params).build()
        }

        if (migrationConfig.chargingWashingMachineOnOffEnabled) {
            new WashingMachineOnOffJobBuilder(params).build()
        }

        if (migrationConfig.chargingWashingMachineRpmEnabled) {
            new ChargingWashingMachineRpmJobBuilder(params).build()
        }

        if (migrationConfig.chargingWashingMachineRpmKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(params).build('_rpm')
        }

        if (migrationConfig.chargingWashingMachineRpmOnOffEnabled) {
            new WashingMachineOnOffJobBuilder(params).build('_rpm')
        }

        if (migrationConfig.chargingWashingMachineBlameEnabled) {
            new ChargingWashingMachineBlameJobBuilder(params).build()
        }

        if (migrationConfig.chargingWashingMachineRpmFullinstallEnabled) {
            new WashingMachineRpmFullinstallJobBuilder(params).build()
        }

        if (migrationConfig.chargingWashingMachineOnDemandEnabled) {
            new ChargingWashingMachineOnDemandJobBuilder(params).build()
        }
    }

    private createCobaWashingMachines(Map params) {
        if (migrationConfig.cobaWashingMachineEnabled) {
            new CobaWashingMachineJobBuilder(params).build()
        }

        if (migrationConfig.cobaWashingMachineKeepAliveEnabled) {
            new OldWashingMachineKeepAliveJobBuilder(params).build()
        }

        if (migrationConfig.cobaWashingMachineOnOffEnabled) {
            new CobaWashingMachineOnOffJobBuilder(params).build()
        }
    }

    private createRmcaWashingMachines(Map params) {
        if (migrationConfig.rmcaWashingMachineEnabled) {
            new RmcaWashingMachineJobBuilder(params).build()
        }

        if (migrationConfig.rmcaWashingMachineKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(params).build()
        }

        if (migrationConfig.rmcaWashingMachineOnOffEnabled) {
            new WashingMachineOnOffJobBuilder(params).build()
        }

        if (migrationConfig.rmcaWashingMachineRpmEnabled) {
            new RmcaWashingMachineRpmJobBuilder(params).build()
        }

        if (migrationConfig.rmcaWashingMachineRpmKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(params).build('_rpm')
        }

        if (migrationConfig.rmcaWashingMachineRpmOnOffEnabled) {
            new WashingMachineOnOffJobBuilder(params).build('_rpm')
        }

        if (migrationConfig.rmcaWashingMachineBlameEnabled) {
            new RmcaWashingMachineBlameJobBuilder(params).build()
        }

        if (migrationConfig.rmcaWashingMachineRpmFullinstallEnabled) {
            new WashingMachineRpmFullinstallJobBuilder(params).build()
        }

        if (migrationConfig.rmcaWashingMachineOnDemandEnabled) {
            new RmcaWashingMachineOnDemandJobBuilder(params).build()
        }
    }

    private createCilWashingMachines(Map params) {
        if (migrationConfig.cilWashingMachineEnabled) {
            new CilWashingMachineJobBuilder(params).build()
        }

        if (migrationConfig.cilWashingMachineKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(params).build()
        }

        if (migrationConfig.cilWashingMachineOnOffEnabled) {
            new CilWashingMachineOnOffJobBuilder(params).build()
        }

        if (migrationConfig.cilWashingMachineOnDemandEnabled) {
            new CilWashingMachineOnDemandJobBuilder(params).build()
        }

        if (migrationConfig.cilWashingMachineEftfEnabled) {
            new CilWashingMachineEftfJobBuilder(params).build()
        }
    }

    private createInvoicingWashingMachines(Map parameters) {
        if (migrationConfig.invoicingWashingMachineEnabled) {
            new InvoicingWashingMachineJobBuilder(parameters).build()
        }

        if (migrationConfig.invoicingWashingMachineKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(parameters).build()
        }

        if (migrationConfig.invoicingWashingMachineRpmFullinstallEnabled) {
            new WashingMachineRpmFullinstallJobBuilder(parameters).build()
        }

        if (migrationConfig.invoicingWashingMachineOnOffEnabled) {
            new InvoicingWashingMachineOnOffJobBuilder(parameters).build()
        }

        if (migrationConfig.invoicingWashingMachineRpmEnabled) {
            new InvoicingWashingMachineRpmJobBuilder(parameters).build()
        }

        if (migrationConfig.invoicingWashingMachineRpmKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(parameters).build('_rpm')
        }

        if (migrationConfig.invoicingWashingMachineRpmOnOffEnabled) {
            new InvoicingWashingMachineRpmOnOffJobBuilder(parameters).build()
        }

        if (migrationConfig.invoicingWashingMachineBlameEnabled) {
            new InvoicingWashingMachineBlameJobBuilder(parameters).build()
        }

        if (migrationConfig.invoicingWashingMachineEftfEnabled) {
            new InvoicingWashingMachineEftfJobBuilder(parameters).build()
        }
        if (migrationConfig.invoicingWashingMachineEftfKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(parameters).build('_eftf')
        }
        if (migrationConfig.invoicingWashingMachineEftfOnOffEnabled) {
            new InvoicingWashingMachineEftfOnOffJobBuilder(parameters).build()
        }
    }

    private createEdmWashingMachines(Map params) {
        if (migrationConfig.edmWashingMachineEnabled) {
            new EdmWashingMachineJobBuilder(params).build()
        }
        if (migrationConfig.edmWashingMachineKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(params).build()
        }
        if (migrationConfig.edmWashingMachineOnOffEnabled) {
            new WashingMachineOnOffJobBuilder(params).build()
        }
    }

    private createMaptWashingMachines(Map params) {
        if (migrationConfig.maptWashingMachineEnabled) {
            new MaptWashingMachineJobBuilder(params).build()
        }
        if (migrationConfig.maptWashingMachineKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(params).build()
        }
        if (migrationConfig.maptWashingMachineOnOffEnabled) {
            new WashingMachineOnOffJobBuilder(params).build()
        }
        if (migrationConfig.maptWashingMachineBlameEnabled) {
            new MaptWashingMachineBlameJobBuilder(params).build()
        }

    }

    private createMsgWashingMachines(Map params) {
        if (migrationConfig.msgWashingMachineBlameEnabled) {
            new MsgWashingMachineBlameJobBuilder(params).build()
        }

    }

    private createCollectionWashingMachines(Map params) {
        if (migrationConfig.collectionWashingMachineEnabled) {
            new CollectionWashingMachineJobBuilder(params).build()
        }
        if (migrationConfig.collectionWashingMachineKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(params).build()
        }
        if (migrationConfig.collectionWashingMachineOnOffEnabled) {
            new WashingMachineOnOffJobBuilder(params).build()
        }
    }

    private createCusWashingMachines(Map params) {
        if (migrationConfig.cusWashingMachineEnabled) {
            new CusWashingMachineJobBuilder(params).build()
        }
        if (migrationConfig.cusWashingMachineKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(params).build()
        }
        if (migrationConfig.cusWashingMachineOnOffEnabled) {
            new WashingMachineOnOffJobBuilder(params).build()
        }
    }

    private createNumWashingMachines(Map params) {
        if (migrationConfig.numWashingMachineEnabled) {
            new NumWashingMachineJobBuilder(params).build()
        }
        if (migrationConfig.numWashingMachineKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(params).build()
        }
        if (migrationConfig.numWashingMachineOnOffEnabled) {
            new WashingMachineOnOffJobBuilder(params).build()
        }
    }

    private createFinanceWashingMachines(Map params) {
        if (migrationConfig.financeWashingMachineEnabled) {
            new FinanceWashingMachineJobBuilder(params).build()
        }
        if (migrationConfig.financeWashingMachineKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(params).build()
        }
        if (migrationConfig.financeWashingMachineOnOffEnabled) {
            new WashingMachineOnOffJobBuilder(params).build()
        }
    }

    private createErmsWashingMachines(Map params) {
        if (migrationConfig.ermsWashingMachineEnabled) {
            new ErmsWashingMachineJobBuilder(params).build()
        }

        if (migrationConfig.ermsWashingMachineKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(params).build()
        }

        if (migrationConfig.ermsWashingMachineOnOffEnabled) {
            new WashingMachineOnOffJobBuilder(params).build()
        }
    }

    private createCpiWashingMachines(Map params) {
        if (migrationConfig.cpiWashingMachineEnabled) {
            new CpiWashingMachineJobBuilder(params).build()
        }

        if (migrationConfig.cpiWashingMachineKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(params).build()
        }

        if (migrationConfig.cpiWashingMachineOnOffEnabled) {
            new WashingMachineOnOffJobBuilder(params).build()
        }
    }

    private createCpmWashingMachines(Map parameters) {
        if (migrationConfig.cpmWashingmachineEnabled) {
            new CpmWashingMachineJobBuilder(parameters).build()
        }

        if (migrationConfig.cpmWashingmachineGraphsEnabled) {
            new CpmWashingMachineGraphsJobBuilder(parameters).build()
        }

        if (migrationConfig.cpmWashingMachineOnOffEnabled) {
            new WashingMachineOnOffJobBuilder(parameters).build()
        }

        if (migrationConfig.cpmWashingmachineKeepaliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(parameters).build()
        }

        if (migrationConfig.cpmWashingMachineBlameEnabled) {
            new CpmWashingMachineBlameJobBuilder(parameters).build()
        }
    }

    private createEpsWashingMachines(Map params) {
        if (migrationConfig.epsWashingMachineEnabled) {
            new EpsWashingMachineJobBuilder(params).build()
        }
        if (migrationConfig.epsWashingMachineKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(params).build()
        }
        if (migrationConfig.epsWashingMachineOnOffEnabled) {
            new WashingMachineOnOffJobBuilder(params).build()
        }
        if (migrationConfig.epsWashingMachineBlameEnabled) {
            new EpsWashingMachineBlameJobBuilder(params).build()
        }
    }

    private createTaxationWashingMachines(Map params){
        if (migrationConfig.taxationWashingMachineEnabled) {
            new TaxationWashingMachineJobBuilder(params).build()
        }
        if (migrationConfig.taxationWashingMachineKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(params).build()
        }
        if (migrationConfig.taxationWashingMachineOnOffEnabled) {
            new WashingMachineOnOffJobBuilder(params).build()
        }
        if (migrationConfig.taxationWashingMachineBlameEnabled) {
            new TaxationWashingMachineBlameJobBuilder(params).build()
        }
    }

    private createUmsWashingMachines(Map params){
        if (migrationConfig.umsWashingMachineEnabled) {
            new UmsWashingMachineJobBuilder(params).build()
        }
        if (migrationConfig.umsWashingMachineKeepAliveEnabled) {
            new WashingMachineKeepAliveJobBuilder(params).build()
        }
        if (migrationConfig.umsWashingMachineOnOffEnabled) {
            new WashingMachineOnOffJobBuilder(params).build()
        }
        if (migrationConfig.umsWashingMachineBlameEnabled) {
            new UmsWashingMachineBlameJobBuilder(params).build()
        }
    }

    private readMigrationConfiguration() {
        ConfigSlurper cs = new ConfigSlurper()
        migrationConfig = cs.parse(dslFactory.readFileFromWorkspace(MIGRATION_CONF))
    }
}
