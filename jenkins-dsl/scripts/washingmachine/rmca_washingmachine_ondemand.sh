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

if [[ -z ${INSTALLNODE} ]]; then
    echo "***** INSTALLNODE need to be set! *****"
    exit
fi

EXECUTION_HOST_PORT=$((${ALLOCATED_PORT}+1))

trap 'set_description_and_send_blamemail_on_exit' EXIT
set_description_and_send_blamemail_on_exit(){
  set +e
  set +v
  set +x

  echo "####################################"
  echo "############# Set tapas url ########"
  echo "####################################"

  # Find tapas url
  tapas_url_cmd="grep 'Web url:' $WORKSPACE/tapasconsole.txt | gawk -F' ' '{print \$6;exit;}'"
  #echo "$tapas_url_cmd"
  tapas_web_url=$(bash -c "$tapas_url_cmd")
  if [ -z "$tapas_web_url" ]; then
      tapas_web_url="https://tapas.epk.ericsson.se/#/suites/RMCA/RMCA%20Washingmachine%20Ondemand"
  fi
  echo "tapas_web_url: $tapas_web_url"
  echo "tapas_web_url=$tapas_web_url" >> env.properties

  echo "JENKINS_DESCRIPTION &nbsp;&nbsp;<a href=\"$tapas_web_url\">Tapas session</a>"

  env | sort
}


if [[ ! -z ${RMCARPM} ]]; then
   TAPAS_RPMRMCA="${RMCARPM}"
   export TAPAS_RPMRMCA
fi

TAPAS_CONFIG=/proj/eta-automation/config/kascmadm/tapas_production.ini
export TAPAS_CONFIG
TAPAS_BASE="/proj/env/tapas/tapas/bin"

BASE_CONFIG_FILE="/proj/eta-automation/tapas/config/rmca/suites/washingmachine_ondemand.xml"
CONFIG_FILE="/proj/eta-automation/tapas/sessions/rmca/washingmachine_ondemand-${TARGETHOST}.xml"
BUILDDESC=`echo ${TARGETHOST} ${CIL} ${INSTALLNODE} ${MSV}`
export BUILDDESC

mkdir -p /proj/eta-automation/tapas/sessions/rmca
cp $BASE_CONFIG_FILE $CONFIG_FILE
cp $CONFIG_FILE tapas-config.xml

echo ""
env
echo ""

logfile="tapasconsole.txt"
test -f $logfile && rm $logfile
touch $logfile



rm -rf ${WORKSPACE}/.repository
#CMD="mvn clean test -Pjive -T8 -U -B -e -X -Dmaven.repo.local=${WORKSPACE}/.repository --settings /proj/eta-automation/maven/kascmadm-settings_arm-charging.xml"

${TAPAS_BASE}/tapas_runner.py --define=__INSTALLNODE__=${INSTALLNODE} \
--define=__MSV__=${MSV} \
--define=__TARGETHOST__=${TARGETHOST} \
--define=__CIL__=${CIL} \
--define=__RMCASTAGINGVERSION__=${RMCASTAGINGVERSION} \
--define=__JIVEVERSION__=${JIVEVERSION} \
--define=__DISPLAYHOME__=${WORKSPACE} \
--define=__EXECUTION_HOST_PORT__=${EXECUTION_HOST_PORT} -v -v -s $CONFIG_FILE >> $logfile 2>&1 &
tapaspid=$!

tail -f $logfile &
logpid=$!

wait $tapaspid
sleep 5
kill $logpid


