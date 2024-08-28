umask 2
PYTHON_EGG_CACHE=$WORKSPACE"/.python-eggs"
export PYTHON_EGG_CACHE
PYTHONUNBUFFERED=1
export PYTHONUNBUFFERED
set -e

export PYTHONPATH="/proj/env/tapas"
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
      tapas_web_url="https://tapas.epk.ericsson.se/#/suites/COBA/COBA%20Washingmachine"
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
  #jive_web_url="https://jive.epk.ericsson.se/#/projects/coba/session-groups/session-group/result?context=Washingmachine&suite=JiveSuite&session-user=kascmadm&test-results-filter=%7B%7D&test-results-session-id=$jive_web_session"
  #echo "jive_web_url: $jive_web_url"
  #echo "jive_web_url=$jive_web_url" >> env.properties

  #echo "JENKINS_DESCRIPTION &nbsp;&nbsp;<a href=\"$tapas_web_url\">Tapas session</a>, <a href=\"$jive_web_url\">Jive session</a>"
  echo "JENKINS_DESCRIPTION &nbsp;&nbsp;<a href=\"$tapas_web_url\">Tapas session</a>"

  env | sort
}

INSTALLNODE="vmx-coba004"
MSV="vmx-coba001"
CIL="vmx-coba002"
TARGETHOST="vmx-coba003"

TAPAS_CONFIG=/proj/eta-automation/config/kascmadm/tapas_production.ini
export TAPAS_CONFIG
TAPAS_BASE="/proj/env/tapas/tapas/bin"

BASE_CONFIG_FILE="/proj/eta-automation/tapas/config/coba/suites/washingmachine.xml"
CONFIG_FILE="/proj/eta-automation/tapas/sessions/coba/washingmachine-${TARGETHOST}.xml"
BUILDDESC=`echo ${TARGETHOST}`
export BUILDDESC

mkdir -p /proj/eta-automation/tapas/sessions/coba
cp $BASE_CONFIG_FILE $CONFIG_FILE
cp $CONFIG_FILE tapas-config.xml

echo ""
env
echo ""

logfile="tapasconsole.txt"
test -f $logfile && rm $logfile
touch $logfile

rm -rf ${WORKSPACE}/.repository

${TAPAS_BASE}/tapas_runner.py --define=__INSTALLNODE__=${INSTALLNODE} \
--define=__MSV__=${MSV} \
--define=__TARGETHOST__=${TARGETHOST} \
--define=__CIL__=${CIL} -k killtimeout -v -v -s $CONFIG_FILE >> $logfile 2>&1 &
tapaspid=$!

tail -f $logfile &
logpid=$!

wait $tapaspid
sleep 5
kill $logpid
