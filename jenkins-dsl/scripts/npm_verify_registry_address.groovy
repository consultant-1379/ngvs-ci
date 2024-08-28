import groovy.json.JsonSlurper

String packageJsonFilePath = 'package.json'
String npmRegistry = System.getenv("NPM_REGISTRY")
String localNpmRegistry = npmRegistry + "-local"

File f = new File(packageJsonFilePath)
def slurper = new JsonSlurper()
def jsonText = f.getText()
json = slurper.parseText(jsonText)

assert "publishConfig" in json : "ERROR! \"publishConfig\" section " +
        "is missing from the 'package.json' file or is empty. It " +
        "must contain a \"registry\" field with the URL of the NPM registry."

assert "registry" in json['publishConfig'] : "ERROR! \"registry\" field " +
        "is missing from the 'publishConfig' section from " +
        "the 'package.json' file."

String actualRegistry = json['publishConfig']['registry']

assert actualRegistry =~ /https:\/\/arm\.[a-z0-9.]*\.ericsson\..*/ :
        "ERROR! NPM registry from the 'package.json' is located " +
        "outside of the Ericsson's ARM server."

assert actualRegistry =~ localNpmRegistry :
        "ERROR! 'package.json' file contains wrong NPM registry address. " +
        "\nExpected value: '" + localNpmRegistry + "'." +
        "\nFound value:    '" + actualRegistry + "'."
