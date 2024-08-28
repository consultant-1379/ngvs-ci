umask 2

if [ ! -f env.properties ]; then
    touch env.properties
fi

PYTHON_EGG_CACHE=$WORKSPACE"/.python-eggs"
export PYTHON_EGG_CACHE
set -e
export PYTHONPATH="/proj/env/tapas"
PYTHONUNBUFFERED=1
export PYTHONUNBUFFERED


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
      tapas_web_url="https://tapas.epk.ericsson.se/#/suites/CPI/CPI%20Non-Functional%20Washingmachine"
  fi
  echo "tapas_web_url: $tapas_web_url"
  echo "tapas_web_url=$tapas_web_url" >> env.properties

  echo "JENKINS_DESCRIPTION &nbsp;&nbsp;<a href=\"$tapas_web_url\">Tapas session</a>"

  env | sort
}

TAPAS_CONFIG=/proj/eta-automation/config/kascmadm/tapas_production.ini
export TAPAS_CONFIG
TAPAS_BASE="/proj/env/tapas/tapas/bin"

BASE_CONFIG_FILE="/proj/eta-automation/tapas/config/invoicing/suites/washingmachine_nfnt.xml"
CONFIG_FILE="/proj/eta-automation/tapas/sessions/invoicing/washingmachine_nfnt-${TARGETHOST}-${TARGETHOST2}.xml"

mkdir -p /proj/eta-automation/tapas/sessions/invoicing
cp $BASE_CONFIG_FILE $CONFIG_FILE
cp $CONFIG_FILE tapas-config.xml

echo ""
env
echo ""

logfile="tapasconsole.txt"
test -f $logfile && rm $logfile
touch $logfile

export NUMBER_OF_CONTROLLERS="$(echo ${TARGETHOST} | awk --field-separator=";" '{ printf NF }')"
export NUMBER_OF_PROCESSORS="$(echo ${TARGETHOST2} | awk --field-separator=";" '{ printf NF }')"
export NUMBER_OF_CILS="$(echo ${CIL} | awk --field-separator=";" '{ printf NF }')"

export CONTROLLERS="$(printf '[%s]' $(echo ${TARGETHOST} | sed -e 's/;/\n/g' | xargs printf '{\"target.host\":\"%s\"},' | sed s'/.$//'))"
export PROCESSORS="$(printf '[%s]' $(echo ${TARGETHOST2} | sed -e 's/;/\n/g' | xargs printf '{\"target.host2\":\"%s\"},' | sed s'/.$//'))"
export CILS="$(printf '[%s]' $(echo ${CIL} | sed -e 's/;/\n/g' | xargs printf '{\"target.cil\":\"%s\"},' | sed s'/.$//'))"

export JIVE_INV_CONTROLLER_COUNT=2
export JIVE_INV_CONTROLLER="$(echo ${TARGETHOST} | awk --field-separator=";" '{ printf $1 }')"
export JIVE_INV_CONTROLLER1="$(echo ${TARGETHOST} | awk --field-separator=";" '{ printf $2 }')"

export JIVE_INV_PROCESSOR_COUNT=2
export JIVE_INV_PROCESSOR="$(echo ${TARGETHOST2} | awk --field-separator=";" '{ printf $1 }')"
export JIVE_INV_PROCESSOR1="$(echo ${TARGETHOST2} | awk --field-separator=";" '{ printf $2 }')"

export JIVE_CIL="$(echo ${CIL} | awk --field-separator=";" '{ printf $1 }')"

export HOST_PREFIX=""
export VMAPI_PREFIX="invoicing."

${TAPAS_BASE}/tapas_runner.py \
--define=__MSV__=${MSV} \
--define=__CONTROLLERS__="${CONTROLLERS}" \
--define=__PROCESSORS__="${PROCESSORS}" \
--define=__CILS__="${CILS}" \
--define=__HOST_PROFILE_PREFIX__=${HOST_PREFIX} \
--define=__VMAPI_PROFILE_PREFIX__=${VMAPI_PREFIX} \
--define=__NUMBER_OF_CONTROLLERS__=${NUMBER_OF_CONTROLLERS} \
--define=__NUMBER_OF_PROCESSORS__=${NUMBER_OF_PROCESSORS} \
--define=__NUMBER_OF_CILS__=${NUMBER_OF_CILS} \
--define=__JIVE_INV_CONTROLLER_COUNT__=${JIVE_INV_CONTROLLER_COUNT} \
--define=__JIVE_INV_CONTROLLER__=${JIVE_INV_CONTROLLER} \
--define=__JIVE_INV_CONTROLLER1__=${JIVE_INV_CONTROLLER1} \
--define=__JIVE_INV_PROCESSOR_COUNT__=${JIVE_INV_PROCESSOR_COUNT} \
--define=__JIVE_INV_PROCESSOR__=${JIVE_INV_PROCESSOR} \
--define=__JIVE_INV_PROCESSOR1__=${JIVE_INV_PROCESSOR1} \
--define=__JIVE_CIL__=${JIVE_CIL} \
-v -v -s $CONFIG_FILE >> $logfile 2>&1 &
tapaspid=$!

tail -f $logfile &
logpid=$!

wait $tapaspid
sleep 5
kill $logpid
