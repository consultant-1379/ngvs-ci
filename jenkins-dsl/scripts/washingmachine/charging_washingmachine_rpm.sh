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
      tapas_web_url="https://tapas.epk.ericsson.se/#/suites/Charging/Charging%20Washingmachine%20RPM"
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
  jive_web_url="https://jive.epk.ericsson.se/#/projects/charging/session-groups/session-group/result?context=WashingmachineRpm&suite=JiveSuite&session-user=kascmadm&test-results-filter=%7B%7D&test-results-session-id=$jive_web_session"
  echo "jive_web_url: $jive_web_url"
  echo "jive_web_url=$jive_web_url" >> env.properties

  echo "JENKINS_DESCRIPTION &nbsp;&nbsp;<a href=\"$tapas_web_url\">Tapas session</a>, <a href=\"$jive_web_url\">Jive session</a>"

  env | sort
}

# Alternate between the different hosts
if [[ "${HOST_SET}" = "alternate" ]]; then
  HOST_SET="$((${BUILD_NUMBER}%2))"
fi
# Use selected host set
if [[ "${HOST_SET}" = "1" ]]; then
  TARGETHOST="vma-cha0018"
  TARGETHOST2="vma-cha0019"
  DLBHOST="vma-cha0020"
  CIL="vma-cha0017"
  MSV="vma-cha0016"
else
  TARGETHOST="vma-cha0018"
  TARGETHOST2="vma-cha0019"
  DLBHOST="vma-cha0020"
  CIL="vma-cha0017"
  MSV="vma-cha0016"
fi

TAPAS_CONFIG=/proj/eta-automation/config/kascmadm/tapas_production.ini
export TAPAS_CONFIG
TAPAS_BASE="/proj/env/tapas/tapas/bin"

BASE_CONFIG_FILE="/proj/eta-automation/tapas/config/charging/suites/installnode/washingmachine_rpm.xml"
CONFIG_FILE="/proj/eta-automation/tapas/sessions/charging/washingmachine_rpm-${TARGETHOST}-${TARGETHOST2}.xml"
BUILDDESC=`echo ${TARGETHOST} ${TARGETHOST2} ${DLBHOST} ${CIL}`
export BUILDDESC

mkdir -p /proj/eta-automation/tapas/sessions/charging
cp $BASE_CONFIG_FILE $CONFIG_FILE
cp $CONFIG_FILE tapas-config.xml

echo ""
env
echo ""

logfile="tapasconsole.txt"
test -f $logfile && rm $logfile
touch $logfile

WM_BLAME_TIMESTAMP=`date +%s%3N`
echo "WM_BLAME_TIMESTAMP: $WM_BLAME_TIMESTAMP"
echo "WM_BLAME_TIMESTAMP=$WM_BLAME_TIMESTAMP" >> env.properties

${TAPAS_BASE}/tapas_runner.py \
--define=__BRANCH__=Washingmachine \
--define=__MSV__=${MSV} \
--define=__TARGETHOST__=${TARGETHOST} \
--define=__TARGETHOST2__=${TARGETHOST2} \
--define=__DLBHOST__=${DLBHOST} \
--define=__CIL__=${CIL} \
--define=__RPM_VERSION__=${CORERPMVERSION} \
--define=__RPM_VERSION2__=${ACCESSRPMVERSION} \
--define=__SERVICERPMVERSION__=${SERVICERPMVERSION} \
--define=__DLBRPMVERSION__=${DLBRPMVERSION} \
--define=__JIVEVERSION__=${JIVEVERSION} \
--define=__JIVE_CONTEXT__=WashingmachineRpm \
--define=__INSTALL_DLB__=true \
-k killtimeout -v -v -s $CONFIG_FILE >> $logfile 2>&1 &

tapaspid=$!

tail -f $logfile &
logpid=$!

wait $tapaspid
sleep 5
kill $logpid
