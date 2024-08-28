mkdir -p ${WORKSPACE}/.tmp
MAVEN_OPTS="-server -Xss1M -Xms128M -Xmx1G -XX:MaxPermSize=128M -verbose:gc -Djava.io.tmpdir=${WORKSPACE}/.tmp/"
export MAVEN_OPTS
MAVEN_HOME="/opt/local/dev_tools/maven/apache-maven-3.0.5"
export MAVEN_HOME
JAVA_HOME="/opt/local/dev_tools/java/jdk1.7.0_45/jre"
export JAVA_HOME
export PATH=$PATH:$JAVA_HOME/bin/
export PATH=$PATH:$MAVEN_HOME/bin/

export EFTF_REST_BASE_URL=http://eftf.epk.ericsson.se/eftfportal

PYTHONUNBUFFERED=True
export PYTHONUNBUFFERED

set -e

#Patch EFTF properties
TARGETHOSTIP=`host ${TARGETHOST} | awk '/has address/ { print $4 }'`
SIMIP=`host ${MSV} | awk '/has address/ { print $4 }'`
sed -i "/sut.host=/c\sut.host=${TARGETHOSTIP}" nfnt/eftf_test.properties
sed -i "/sim.url=/c\sim.url=${SIMIP}" nfnt/eftf_test.properties
sed -i "/deploy.agent=/c\deploy.agent=true" nfnt/eftf_test.properties

rm -rf ${WORKSPACE}/.repository
mvn test -pl=nfnt -Dtest=AutoStabilityTest test -T8 -U -B -e -X -Dmaven.repo.local=${WORKSPACE}/.repository -s /proj/eta-automation/maven/kascmadm-settings_arm-cil.xml
