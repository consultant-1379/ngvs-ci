#!/bin/sh
set -x
set -e
set -u

# This script runs the DSL plugin from commandline without jenkins.
# Generates xml files for inspection and smoketests the dsl syntax.
# Builds dsl plugin the first time it is run.
# Must be run from the jenkins-dsl root dir (same as this script is in).

WORKDIR=`/bin/pwd`

# Must be a git version with proper ssl support
GIT=${GIT:-/proj/eta-tools/gnu-bundle/linux/bin/git}
test -x $GIT || GIT=git

# Mocked env variable for DSL jobs:
JENKINS_URL='https://eta.epk.ericsson.se/jenkins/'
export JENKINS_URL

JAVA_TOOL_OPTIONS="-Xmx4G -Xms16M"
export JAVA_TOOL_OPTIONS

JAVA_HOME=${JAVA_HOME:-/opt/local/dev_tools/java/x64/latest-1.8}
test -d $JAVA_HOME || JAVA_HOME=/usr
export JAVA_HOME

PATH=${JAVA_HOME}/bin:${PATH}
export PATH

GROOVY_JAR=${GROOVY_JAR:-/proj/eta-tools/groovy/2.4.4/lib/groovy-2.4.4.jar}

DSL_VERSION=1.37
DSL_JAR=${WORKDIR}/job-dsl-plugin/job-dsl-core/build/libs/job-dsl-core-${DSL_VERSION}-standalone.jar
if ! test -s $DSL_JAR; then
    test -d job-dsl-plugin || $GIT clone https://github.com/jenkinsci/job-dsl-plugin.git
    cd job-dsl-plugin
    $GIT checkout -b job-dsl-${DSL_VERSION} job-dsl-${DSL_VERSION}
    ./gradlew :job-dsl-core:oneJar
    cd ${WORKDIR}
fi

CLASSPATH=${GROOVY_JAR}
export CLASSPATH

cd src/main/groovy
test -L scripts || ln -s ../../../scripts
find ${WORKDIR}/jobs/ -name '*.groovy' | while read line; do
	java -jar ${DSL_JAR} $line
done

# TODO: This may be a more correct invocation:
# cd job-dsl-plugin/job-dsl-core/
# ln -s ../../src/main/groovy/com
# ln -s ../../jobs/bssJobs.groovy
# ./gradlew run -Pargs=bssJobs.groovy

