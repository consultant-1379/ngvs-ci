package com.ericsson.bss.util

import com.ericsson.bss.AbstractJobBuilder

@Singleton
public class RepositoryMavenConfig {
    HashMap<String, String> configMap = new HashMap<String, String>() { {
        put("charging/com.ericsson.bss.rm.charging.access.tools", "3.2.2")
    } }

    public void put(String repository, String version) {
        configMap.put(repository, version)
    }

    public String getMavenVersion(String repository, String branchName) {
        String mavenVersion = configMap.get(repository)!=null?
                configMap.get(repository):
                AbstractJobBuilder.MAVEN_VERSION

        //Older branches for some TPG's has dependencies against maven 3.2.1. Problems is often seen when doing releases.
        //https://eta.epk.ericsson.se/helpdesk/view.php?id=7715
        if (repository != null && branchName != null && branchName.contains('release') && (
        repository.contains('charging/com.ericsson.bss.rm.charging.access')
        || repository.contains('collection/com.ericsson.bss.rm.collection')
        || repository.contains('erms/com.ericsson.bss.rm.erms')
        || repository.contains('finance/com.ericsson.bss.rm.finance')
        || repository.contains('charging/com.ericsson.bss.rm.coba')
        ) && mavenVersion == AbstractJobBuilder.MAVEN_VERSION) {
            mavenVersion = '3.2.1'
        }

        return mavenVersion
    }
}
