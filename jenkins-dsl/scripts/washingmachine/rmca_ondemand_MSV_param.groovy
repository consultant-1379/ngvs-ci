if (CLUSTER.equals("INPUT STRING")) {
    return "<input name=\"value\" value=\"\" class=\"setting-input\" type=\"text\">"
} else {
    def file = new File('/proj/eta-automation/jenkins/kascmadm/job_config/', 'rmca_cluster_list')

    if (!file.exists()) {
        return ""
    }

    def parser = new XmlParser()
    def clusters = parser.parse(file)

    def msvHtml = ""

    clusters.children().find {
        if (CLUSTER.equals(it.@name)) {
            msvHtml = "<input name=\"value\" value=\"" + it.@msv + "\" class=\"setting-input\" type=\"text\" disabled>"
            return true
        }
        return false
    }
    return msvHtml
}
