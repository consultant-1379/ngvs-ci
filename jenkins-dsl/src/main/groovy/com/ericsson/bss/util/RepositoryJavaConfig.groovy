package com.ericsson.bss.util

import com.ericsson.bss.AbstractJobBuilder

@Singleton
public class RepositoryJavaConfig {

    HashMap<String, String> configMap = new HashMap<String, String>() { {
            put("charging/com.ericsson.bss.rm.charging", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.access.bundle", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.access.common", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.access.configmanager", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.access.integration", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.access.integrationtest", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.access.oam", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.access.oam.fm", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.access.oam.log", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.access.oam.pm", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.access.plugins", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.access.productiondependencies", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.access.services", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.access.tools", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.access.trafficcontroller", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.access.fnt", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.bucketmanagement", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.bundle", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.capability", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.cel", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.cli", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.common", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.common.oam", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.config", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.config-api", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.configcache", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.configurations", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.core.bundle", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.core.clamshell", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.core.genericinterfaceparameters", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.core.ui", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.customermanagement", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.dataaccess", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.dlb", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.dynamicfunction", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.event", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.functioncontrol", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.globalconfig", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.integration", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.integration.config", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.integrationtest", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.invoiceaggregation", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.invoiceaggregation.customerbillingcycle", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.oam", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.periodicaction", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.productiondependencies", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.productselection", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.productstatus", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.race", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.reasoncode", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.refill", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.releasenotegenerator", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.rf.core", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.routing", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.runtimeflow", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.serializer", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.servicedetermination", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.serviceprovider", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.services", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.session", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.shared", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.transport", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("charging/com.ericsson.bss.rm.charging.vre", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("common_osgi-akka", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("common_osgi-cilutils", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("common_osgi-functioncontrol", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("common_osgi-oam", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("common_osgi-security", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("common_osgi-trace", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("jive/charging", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("erms/com.ericsson.bss.rm.erms.integrationtest", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("erms/com.ericsson.bss.rm.erms.core", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("eftf/erms", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("erms/com.ericsson.bss.rm.erms.opd", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("erms/com.ericsson.bss.rm.erms.productiondependencies", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("erms/com.ericsson.bss.rm.erms.restapi", AbstractJobBuilder.SONAR_JDK_VERSION)
            put("erms/com.ericsson.bss.rm.erms.utilities", AbstractJobBuilder.SONAR_JDK_VERSION)
        }
    }

    public void put(String repository, String version) {
        configMap.put(repository, version)
    }

    public String getJavaVersion(String repository) {
        return configMap.get(repository) != null ? configMap.get(repository) : AbstractJobBuilder.JDK_VERSION
    }
}
