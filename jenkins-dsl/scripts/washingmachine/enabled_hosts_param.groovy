import jenkins.model.*
def instance = Jenkins.getInstance()
def jobname = "<PROJECT_NAME>"
def job = instance.getItem(jobname)
def choices = job.getProperty(hudson.model.ParametersDefinitionProperty.class).getParameterDefinitions().get(0).getChoices()
def list = choices.collect() // Copy the list so we don't change any parameters from the WM
list.add('none')

File propsFile = new File('/proj/eta-automation/jenkins/kascmadm/job_config/' + jobname + '_params.properties')
if (!propsFile.exists()) {
    return list
}
Properties props = new Properties()
props.load(propsFile.newDataInputStream())

int index = 0
String storedValue = props.get('HOST_SET')
list.find {
    if (it == storedValue) {
        list[index] = it + ':selected'
        return true
    }
    ++index
    return false
}
return list