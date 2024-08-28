#!/bin/bash
umask 2
PYTHON_EGG_CACHE=$WORKSPACE"/.python-eggs"
export PYTHON_EGG_CACHE
PYTHONUNBUFFERED=1
export PYTHONUNBUFFERED
set -e

export PYTHONPATH="/proj/env/tapas"

trap 'set_description_on_exit' EXIT
set_description_on_exit(){
  set +e
  set +v
  set +x

  echo "#############################################"
  echo "############### Set tapas url ###############"
  echo "#############################################"

  # Find tapas url
  tapas_url_cmd="grep 'Web url:' $WORKSPACE/tapasconsole.txt | gawk -F' ' '{print \$6;exit;}'"
  #echo "$tapas_url_cmd"
  tapas_web_url=$(bash -c "$tapas_url_cmd")
  if [ -z "$tapas_web_url" ]; then
      tapas_web_url="https://tapas.epk.ericsson.se/#/suites/CIL/CIL%20Washingmachine"
  fi
  echo "tapas_web_url: $tapas_web_url"
  echo "tapas_web_url=$tapas_web_url" >> env.properties

  echo "JENKINS_DESCRIPTION &nbsp;&nbsp;<a href=\"$tapas_web_url\">Tapas session</a>"

  env | sort
}

TAPAS_CONFIG=/proj/eta-automation/config/kascmadm/tapas_production.ini
export TAPAS_CONFIG
TAPAS_BASE="/proj/env/tapas/tapas/bin"

BASE_CONFIG_FILE="/proj/eta-automation/tapas/config/cil/suites/installnode/washingmachine_ovf.xml"
CONFIG_FILE="/proj/eta-automation/tapas/sessions/cil/washingmachine_ovf-${TARGETHOST}.xml"

INSTALLNODE="vmx-cil019"
TARGETHOST="vmx-cil009"
TARGETHOST2="vmx-cil010"
TARGETHOST3="vmx-cil011"
TARGETHOST4="vmx-cil020"

mkdir -p /proj/eta-automation/tapas/sessions/cil
cp $BASE_CONFIG_FILE $CONFIG_FILE
cp $CONFIG_FILE tapas-config.xml

echo ""
env
echo ""

logfile="tapasconsole.txt"
test -f $logfile && rm $logfile
touch $logfile

PARAMS=""
if [ ! -z "$RELEASEVERSION" ]; then
  PARAMS="$PARAMS --define=__RELEASEVERSION__=${RELEASEVERSION}"
fi

${TAPAS_BASE}/tapas_runner.py --define=__TARGETHOST__=${TARGETHOST} \
--define=__TARGETHOST2__=${TARGETHOST2} \
--define=__TARGETHOST3__=${TARGETHOST3} \
--define=__TARGETHOST4__=${TARGETHOST4} \
--define=__INSTALLNODE__="${INSTALLNODE}" \
${PARAMS} \
--define=__MSV__=${MSV} -k killtimeout -v -v -s $CONFIG_FILE >> $logfile 2>&1 &
tapaspid=$!

tail -f $logfile &
logpid=$!
wait $tapaspid
kill $logpid

