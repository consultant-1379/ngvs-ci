#!/bin/bash
umask 2
PYTHON_EGG_CACHE=$WORKSPACE"/.python-eggs"
export PYTHON_EGG_CACHE
PYTHONUNBUFFERED=1
export PYTHONUNBUFFERED
set -e

export NUMBER_OF_CLUSTERS=1

# Alternate between the different hosts
if [[ "${HOST_SET}" = "alternate" ]]; then
  HOST_SET="$(($((${BUILD_NUMBER}%$NUMBER_OF_CLUSTERS))+1))"
fi


MSV="vma-rmca0007"
CIL="vma-rmca0008"
TARGETHOST="vma-rmca0009"


export PYTHONPATH="/proj/env/tapas"
TAPAS_CONFIG=/proj/eta-automation/config/kascmadm/tapas_production.ini
export TAPAS_CONFIG
TAPAS_BASE="/proj/env/tapas/tapas/bin"

BASE_CONFIG_FILE="/proj/eta-automation/tapas/config/rmca/suites/washingmachine_rpm.xml"
CONFIG_FILE="/proj/eta-automation/tapas/sessions/rmca/washingmachine_rpm-${TARGETHOST}.xml"

mkdir -p /proj/eta-automation/tapas/sessions/rmca
cp $BASE_CONFIG_FILE $CONFIG_FILE
cp $CONFIG_FILE tapas-config.xml

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
      tapas_web_url="https://tapas.epk.ericsson.se/#/suites/RMCA/RMCA%20Washingmachine%20RPM"
  fi
  echo "tapas_web_url: $tapas_web_url"
  echo "tapas_web_url=$tapas_web_url" >> env.properties

  # Find jive url
  jive_url_cmd="grep 'Jive session id: ' $WORKSPACE/tapasconsole.txt | gawk -F'id: ' '{print \$2;exit;}'"
  #echo "$jive_url_cmd"
  jive_web_session=$(bash -c "$jive_url_cmd")
  if [ -z "$jive_web_session" ]; then
      jive_web_session="latest"
  fi
  jive_web_url="https://jive.epk.ericsson.se/#/projects/rmca/session-groups/session-group/result?context=WashingmachineRpm&suite=RmcaSuite&session-user=kascmadm&test-results-filter=%7B%7D&test-results-session-id=$jive_web_session"
  echo "jive_web_url: $jive_web_url"
  echo "jive_web_url=$jive_web_url" >> env.properties


  echo "JENKINS_DESCRIPTION &nbsp;&nbsp;<a href=\"$tapas_web_url\">Tapas session</a>, <a href=\"$jive_web_url\">Jive session</a>"

  env | sort
}


echo ""
env
echo ""

logfile="tapasconsole.txt"
test -f $logfile && rm $logfile
touch $logfile

${TAPAS_BASE}/tapas_runner.py --define=__BRANCH__=Washingmachine \
--define=__TARGETHOST__=${TARGETHOST} \
--define=__CIL__=${CIL} \
--define=__MSV__=${MSV} \
--define=__DISPLAYHOME__=${WORKSPACE} \
--define=__EXECUTION_HOST_PORT__=${EXECUTION_HOST_PORT} -k killtimeout -v -v -s $CONFIG_FILE >> $logfile 2>&1 &
tapaspid=$!

tail -f $logfile &
logpid=$!
wait $tapaspid
kill $logpid
