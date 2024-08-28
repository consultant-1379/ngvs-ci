def versions = []
def metadata = new XmlSlurper().parse("https://arm.epk.ericsson.se/artifactory/proj-rmca-release-local/com/ericsson/bss/rmca/integration/rmcapackage/maven-metadata.xml")
metadata.versioning.versions.version.each {
    versions.add(it.text())
}

metadata = new XmlSlurper().parse("https://arm.epk.ericsson.se/artifactory/proj-rmca-dev-local/com/ericsson/bss/rmca/integration/rmcapackage/maven-metadata.xml")
metadata.versioning.versions.version.each {
    if (!versions.contains(it.text().minus('-SNAPSHOT'))) {
        versions.add(it.text())
    }
}

versions.sort { a, b -> b <=> a }
versions.add(0, 'LATEST');

return versions
