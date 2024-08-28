String jobname = "<PROJECT_NAME>"
def list = ["Enable", "Disable"]

File propsFile = new File('/proj/eta-automation/jenkins/kascmadm/job_config/' + jobname + '_params.properties')
if (!propsFile.exists()) {
    return list
}
Properties props = new Properties()
props.load(propsFile.newDataInputStream())

int index = 0
String storedValue = "Enable"
if (props.get('JOB_ENABLED') == "false") {
    storedValue = "Disable"
}

list.find {
    if (it == storedValue) {
        list[index] = it + ':selected'
        return true
    }
    ++index
    return false
}
return list