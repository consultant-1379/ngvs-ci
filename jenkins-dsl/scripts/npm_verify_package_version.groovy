import groovy.json.JsonSlurper

String packageJsonFilePath = "package.json"

File f = new File(packageJsonFilePath)
def slurper = new JsonSlurper()
def jsonText = f.getText()
json = slurper.parseText(jsonText)

assert "version" in json : "ERROR! \"version\" section " +
        "is missing from the 'package.json' file or is empty. It " +
        "must contain the version of the package."

assert json["version"] =~ /[0-9]+.[0-9]+.[0-9]+-SNAPSHOT/ : "ERROR! Wrong format " +
        "of the 'package.json' file. Expected value always meets " +
        "the following pattern: X.Y.Z-SNAPSHOT . Found value: " + json["version"]
