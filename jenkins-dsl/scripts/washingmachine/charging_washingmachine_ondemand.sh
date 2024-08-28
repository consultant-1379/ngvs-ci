umask 2
PYTHON_EGG_CACHE=$WORKSPACE"/.python-eggs"
export PYTHON_EGG_CACHE
PYTHONUNBUFFERED=1
export PYTHONUNBUFFERED
set -e

export PYTHONPATH="/proj/env/tapas"

MAVEN_OPTS="-server -Xss1M -Xms128M -Xmx1G -XX:MaxPermSize=128M -verbose:gc -Djava.io.tmpdir=${WORKSPACE}/.tmp/"
export MAVEN_OPTS
MAVEN_HOME="/opt/local/dev_tools/maven/apache-maven-3.0.5"
export MAVEN_HOME
JAVA_HOME="/opt/local/dev_tools/java/jdk1.7.0_45/jre"
export JAVA_HOME
export PATH=$PATH:$JAVA_HOME/bin/
export PATH=$PATH:$MAVEN_HOME/bin/
PYTHONUNBUFFERED=1
export PYTHONUNBUFFERED
export PATH="/proj/env/jive/:$PATH"


trap 'set_description_and_send_blamemail_on_exit' EXIT
set_description_and_send_blamemail_on_exit(){
  set +e
  set +v
  set +x

  echo "#############################################"
  echo "############# Set tapas and jive url ########"
  echo "#############################################"

  # Find tapas url
  tapas_url_cmd="grep 'Web url:' $WORKSPACE/tapasconsole.txt | gawk -F' ' '{print \$6;exit;}'"
  #echo "$tapas_url_cmd"
  tapas_web_url=$(bash -c "$tapas_url_cmd")
  if [ -z "$tapas_web_url" ]; then
      tapas_web_url="https://tapas.epk.ericsson.se/#/suites/Charging/Charging%20washingmachine%20ondemand"
  fi
  echo "tapas_web_url: $tapas_web_url"
  echo "tapas_web_url=$tapas_web_url" >> env.properties

  # Find jive url
  #jive_url_cmd="grep 'Test session created:' $WORKSPACE/tapasconsole.txt | gawk -F'\"' '{print \$4;exit;}'"
  ##echo "$jive_url_cmd"
  #jive_web_session=$(bash -c "$jive_url_cmd")
  #if [ -z "$jive_web_session" ]; then
  #    jive_web_session="latest"
  #fi
  #jive_web_url="https://jive.epk.ericsson.se/#/projects/charging/session-groups/session-group/$jive_web_session?context=JiveCharging&current-project=charging&suite=ChargingFullTest&session-user=kascmadm"
  #echo "jive_web_url: $jive_web_url"
  #echo "jive_web_url=$jive_web_url" >> env.properties

  #echo "JENKINS_DESCRIPTION &nbsp;&nbsp;<a href=\"$tapas_web_url\">Tapas session</a>, <a href=\"$jive_web_url\">Jive session</a>"
  echo "JENKINS_DESCRIPTION &nbsp;&nbsp;<a href=\"$tapas_web_url\">Tapas session</a>"

  env
}

if [[ ! -z ${RPMCORE} ]]; then
   TAPAS_RPMCORE="${RPMCORE}"
   export TAPAS_RPMCORE
fi
if [[ ! -z ${RPMACCESS} ]]; then
   TAPAS_RPMACCESS="${RPMACCESS}"
   export TAPAS_RPMACCESS
fi
if [[ ! -z ${RPMDLB} ]]; then
   TAPAS_RPMDLB="${RPMDLB}"
   export TAPAS_RPMDLB
fi

if [[ "${COREOVFVERSION}" = "LATEST" ]]; then
  COREOVFVERSION="999.9.9"
fi

if [[ "${ACCESSOVFVERSION}" = "LATEST" ]]; then
  ACCESSOVFVERSION="999.9.9"
fi

if [[ "${DLBOVFVERSION}" = "LATEST" ]]; then
  DLBOVFVERSION="999.9.9"
fi

TAPAS_CONFIG=/proj/eta-automation/config/kascmadm/tapas_production.ini
export TAPAS_CONFIG
TAPAS_BASE="/proj/env/tapas/tapas/bin"

BASE_CONFIG_FILE="/proj/eta-automation/tapas/config/charging/suites/installnode/washingmachine_ondemand.xml"
CONFIG_FILE="/proj/eta-automation/tapas/sessions/charging/washingmachine_ondemand-${TARGETHOST}-${TARGETHOST2}.xml"
BUILDDESC=`echo ${TARGETHOST} ${TARGETHOST2} ${CIL} ${INSTALLNODE} ${MSV}`
export BUILDDESC

mkdir -p /proj/eta-automation/tapas/sessions/charging
cp $BASE_CONFIG_FILE $CONFIG_FILE
cp $CONFIG_FILE tapas-config.xml

echo ""
env
echo ""

# Set host config dir
HOST_CONFIG_DIR="/proj/eta-automation/tapas/config/${PRODUCT}/config/"
export HOST_CONFIG_DIR

logfile="tapasconsole.txt"
test -f $logfile && rm $logfile
touch $logfile


# Edit jive config
#sed -i /chargingAccessServer=/c\chargingAccessServer=${TARGETHOST2} ${WORKSPACE}/tests/src/main/resources/com/ericsson/charging/base/charging.cfg
#sed -i /chargingCoreServer=/c\chargingCoreServer=${TARGETHOST} ${WORKSPACE}/tests/src/main/resources/com/ericsson/charging/base/charging.cfg

rm -rf ${WORKSPACE}/.repository
#CMD="mvn clean test -Pjive -T8 -U -B -e -X -Dmaven.repo.local=${WORKSPACE}/.repository --settings /proj/eta-automation/maven/kascmadm-settings_arm-charging.xml"

${TAPAS_BASE}/tapas_runner.py --define=__INSTALLNODE__=${INSTALLNODE} \
--define=__MSV__=${MSV} \
--define=__TARGETHOST__=${TARGETHOST} \
--define=__TARGETHOST2__=${TARGETHOST2} \
--define=__DLBHOST__=${DLBHOST} \
--define=__CIL__=${CIL} \
--define=__COREVERSION__=${CORESTAGINGVERSION} \
--define=__ACCESSVERSION__=${ACCESSSTAGINGVERSION} \
--define=__DLBVERSION__=${DLBSTAGINGVERSION} \
--define=__VMAPI_PROFILE_PREFIX__=${VMAPIPROFILE} \
--define=__HOST_PROFILE_PREFIX__=${HOSTPROFILE} \
--define=__JIVEVERSION__=${JIVEVERSION} \
--define=__COREOVFVERSION__=${COREOVFVERSION} \
--define=__ACCESSOVFVERSION__=${ACCESSOVFVERSION} \
--define=__DLBOVFVERSION__=${DLBOVFVERSION} -v -v -s $CONFIG_FILE >> $logfile 2>&1 &
tapaspid=$!

tail -f $logfile &
logpid=$!

wait $tapaspid
sleep 5
kill $logpid
