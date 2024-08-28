String jobname = "<PROJECT_NAME>"
String paramName = "<PARAMETER_NAME>"

File propsFile = new File('/proj/eta-automation/jenkins/kascmadm/job_config/' + jobname + '_params.properties')
if (!propsFile.exists()) {
    return "LATEST"
}
Properties props = new Properties()
props.load(propsFile.newDataInputStream())

return '<input name="value" value="' + props.get(paramName) + '" class="setting-input" type="text">'