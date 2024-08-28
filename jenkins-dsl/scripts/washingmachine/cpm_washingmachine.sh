#!/bin/bash
umask 2
PYTHON_EGG_CACHE=$WORKSPACE"/.python-eggs"
export PYTHON_EGG_CACHE
PYTHONUNBUFFERED=1
export PYTHONUNBUFFERED
set -e

export PYTHONPATH="/proj/env/tapas"
export JAVA_HOME="/opt/local/dev_tools/java/x64/latest-1.7"
export PATH="/opt/local/dev_tools/maven/apache-maven-3.2.3/bin:${JAVA_HOME}/bin:/proj/env/bin:$PATH"

TAPAS_CONFIG=/proj/eta-automation/config/kascmadm/tapas_production.ini
export TAPAS_CONFIG
TAPAS_BASE="/proj/env/tapas/tapas/bin"

HOST_PROFILE_PREFIX="washing."
HOST_INSTALLNODE="vmx-cpmka-011"
HOST_MSV="vmx-cpmka-012"
HOST_CIL="vmx-cpmka-013"
HOST_CPM="vmx-cpmka-014"
#HOST_PROFILE_PREFIX="release."
#HOST_INSTALLNODE="vmx-cpmka-001"
#HOST_MSV="vmx-cpmka-002"
#HOST_CIL="vmx-cpmka-003"
#HOST_CPM="vmx-cpmka-004"

BASE_CONFIG_FILE="/proj/eta-automation/tapas/config/cpm/suites/installnode/washingmachine.xml"
CONFIG_FILE="/proj/eta-automation/tapas/sessions/cpm/washingmachine-${HOST_CPM}.xml"
BUILDDESC=`echo ${TARGETHOST} ${TARGETHOST2}`
export BUILDDESC

mkdir -p /proj/eta-automation/tapas/sessions/cpm
cp $BASE_CONFIG_FILE $CONFIG_FILE
cp $CONFIG_FILE tapas-config.xml


trap 'set_description_and_send_blamemail_on_exit' EXIT
set_description_and_send_blamemail_on_exit(){
  set +e
  set +v
  set +x

  echo "#############################################"
  echo "############## Set tapas ####################"
  echo "#############################################"

  # Find tapas url
  tapas_url_cmd="grep 'Web url:' $WORKSPACE/tapasconsole.txt | gawk -F' ' '{print \$6;exit;}'"
  #echo "$tapas_url_cmd"
  tapas_web_url=$(bash -c "$tapas_url_cmd")
  if [ -z "$tapas_web_url" ]; then
      tapas_web_url="https://tapas.epk.ericsson.se/#/suites/CPM/CPM%20Washingmachine"
  fi
  echo "tapas_web_url: $tapas_web_url"
  echo "tapas_web_url=$tapas_web_url" >> $WORKSPACE/env.properties

  # Find jive url
  jive_url_cmd="grep 'Jive session id: ' $WORKSPACE/tapasconsole.txt | gawk -F'id: ' '{print \$2;exit;}'"
  #echo "$jive_url_cmd"
  jive_web_session=$(bash -c "$jive_url_cmd")
  if [ -z "$jive_web_session" ]; then
      jive_web_session="latest"
  fi
  jive_web_url="https://jive.epk.ericsson.se/#/projects/cpm/session-groups/session-group/result?context=Washingmachine&suite=CpmSuite&session-user=kascmadm&test-results-filter=%7B%7D&test-results-session-id=$jive_web_session"
  echo "jive_web_url: $jive_web_url"
  echo "jive_web_url=$jive_web_url" >> $WORKSPACE/env.properties

  ciscat_result=$(grep 'CIS-CAT Score: ' $WORKSPACE/tapasconsole.txt | cut -d ' ' -f 6-)
  if [ -z "$ciscat_result" ]; then
      ciscat_result="N/A"
  fi
  echo "CISCAT_RESULT=$ciscat_result" >> $WORKSPACE/env.properties
  echo $CISCAT_RESULT

  echo "JENKINS_DESCRIPTION &nbsp;&nbsp;<a href=\"$tapas_web_url\">Tapas session</a>, <a href=\"$jive_web_url\">Jive session</a>, CPM=$HOST_CPM"

  env | sort
}


logfile="tapasconsole.txt"
test -f $logfile && rm $logfile
touch $logfile

rm -rf ${WORKSPACE}/.repository

${TAPAS_BASE}/tapas_runner.py \
--define=__MSV__="$HOST_MSV" \
--define=__TARGETHOST__="$HOST_CPM" \
--define=__CIL__="$HOST_CIL" \
--define=__FOLDER__="snapshot" \
--define=__HOST_PROFILE_PREFIX__="$HOST_PROFILE_PREFIX" \
--define=__PRODUCTION_MODE__=true \
--define=__JIVE_CONTEXT__=Washingmachine \
--define=__OVFVERSION__=LATEST -k killtimeout -v -v -s $CONFIG_FILE
 >> $logfile 2>&1 &
tapaspid=$!

tail -f $logfile &
logpid=$!

wait $tapaspid
sleep 5
kill $logpid

###############################
# Performance data collection #
###############################
DATE="$(date "+%s")"
# Camunda flow logs
if [[ -r $WORKSPACE/temp/jmeterPerformance2.log ]]; then
    cp $WORKSPACE/temp/jmeterPerformance2.log /proj/kacx/artifacts/rrdtool/cpm/performance/data/jmeterPerformance2.log.$DATE
fi
if [[ -r $WORKSPACE/temp/vmware_statistics2.log ]]; then
    cp $WORKSPACE/temp/vmware_statistics2.log /proj/kacx/artifacts/rrdtool/cpm/performance/data/vmware_statistics2.log.$DATE
fi

##############################
# Expand jmeter result files #
##############################
if [ -f "${WORKSPACE}/temp/result2.tar.gz" ]; then
    mkdir -p /proj/kacx/artifacts/rrdtool/cpm/performance/jmeter
    rm -rf /proj/kacx/artifacts/rrdtool/cpm/performance/jmeter/*
    cd /proj/kacx/artifacts/rrdtool/cpm/performance/jmeter
    tar -xvf "${WORKSPACE}/temp/result2.tar.gz"
fi