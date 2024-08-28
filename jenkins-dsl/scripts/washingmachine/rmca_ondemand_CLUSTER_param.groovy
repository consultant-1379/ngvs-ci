def file = new File('/proj/eta-automation/jenkins/kascmadm/job_config/', 'rmca_cluster_list')

if (!file.exists()) {
    return ""
}

def parser = new XmlParser()
def clusters = parser.parse(file)

def list = []

clusters.children().each {
    list.add(it.@name)
}

list.add(0, "INPUT STRING")

return list
