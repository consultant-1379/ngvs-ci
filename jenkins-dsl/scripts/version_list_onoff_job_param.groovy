String jobname = "<PROJECT_NAME>"
String paramName = "<PARAMETER_NAME>"
def version_list = []
def metadata = new XmlSlurper().parse("<VERSION_URL>")
metadata.versioning.versions.version.each{
    version_list.add(it.text())
}

version_list.sort{a,b-> b<=>a}
<LATEST>

File propsFile = new File('/proj/eta-automation/jenkins/kascmadm/job_config/' + jobname + '_params.properties')
if (!propsFile.exists()) {
    return version_list
}

Properties props = new Properties()
props.load(propsFile.newDataInputStream())

int index = 0
String storedValue = props.get(paramName)
version_list.find {
    if (it == storedValue) {
        version_list[index] = it + ":selected"
        return true
    }
    ++index
    return false
}

return version_list