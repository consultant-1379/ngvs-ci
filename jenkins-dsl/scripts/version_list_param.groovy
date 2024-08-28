def version_list = []
def metadata = new XmlSlurper().parse("<VERSION_URL>")
metadata.versioning.versions.version.each {
    if (!it.text().contains("-rc")) {
        version_list.add(it.text())
    }
}

version_list.sort{a,b-> b<=>a}
<LATEST>

return version_list
