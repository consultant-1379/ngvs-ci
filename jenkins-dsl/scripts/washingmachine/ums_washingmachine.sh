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
      tapas_web_url="https://tapas.epk.ericsson.se/#/suites/UMS/UMS%20Washingmachine"
  fi
  echo "tapas_web_url: $tapas_web_url"
  echo "tapas_web_url=$tapas_web_url" >> env.properties

  # Find jive url
  jive_web_url="N/A"
  echo "jive_web_url=$jive_web_url" >> env.properties
  echo $jive_web_url

  # Get CIS-CAT score
  ciscat_result=$(grep 'CIS-CAT Score: ' $WORKSPACE/tapasconsole.txt | cut -d ' ' -f 6-)
  if [ -z "$ciscat_result" ]; then
      ciscat_result="N/A"
  fi

  echo "CISCAT_RESULT=$ciscat_result" >> env.properties
  echo $CISCAT_RESULT

  echo "JENKINS_DESCRIPTION &nbsp;&nbsp;<a href=\"$tapas_web_url\">Tapas session</a>"

  env | sort
}


MSV="vma-ums0001"
CIL="vma-ums0002"
TARGETHOST="vma-ums0003"


TAPAS_CONFIG=/proj/eta-automation/config/kascmadm/tapas_production.ini
export TAPAS_CONFIG
TAPAS_BASE="/proj/env/tapas/tapas/bin"

BASE_CONFIG_FILE="/proj/eta-automation/tapas/config/ums/suites/washingmachine.xml"
CONFIG_FILE="/proj/eta-automation/tapas/sessions/ums/washingmachine-${TARGETHOST}.xml"
BUILDDESC=`echo ${TARGETHOST}`
export BUILDDESC

mkdir -p /proj/eta-automation/tapas/sessions/ums
cp $BASE_CONFIG_FILE $CONFIG_FILE
cp $CONFIG_FILE tapas-config.xml

echo ""
env
echo ""

logfile="tapasconsole.txt"
test -f $logfile && rm $logfile
touch $logfile

rm -rf ${WORKSPACE}/.repository

${TAPAS_BASE}/tapas_runner.py \
--define=__MSV__=${MSV} \
--define=__CIL__=${CIL} \
--define=__TARGETHOST__=${TARGETHOST} \
--define=__OVFVERSION__=LATEST \
-k killtimeout \
-v -v -s $CONFIG_FILE >> $logfile 2>&1 &
tapaspid=$!

tail -f $logfile &
logpid=$!

wait $tapaspid
sleep 5
kill $logpid

