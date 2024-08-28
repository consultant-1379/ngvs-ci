#!/bin/bash
umask 2
PYTHON_EGG_CACHE=$WORKSPACE"/.python-eggs"
export PYTHON_EGG_CACHE
PYTHONUNBUFFERED=1
export PYTHONUNBUFFERED
set -e

export PYTHONPATH="/proj/env/tapas"
TAPAS_CONFIG=/proj/eta-automation/config/kascmadm/tapas_production.ini
export TAPAS_CONFIG
TAPAS_BASE="/proj/env/tapas/tapas/bin"

BASE_CONFIG_FILE="/proj/eta-automation/tapas/config/cil/suites/installnode/washingmachine_ondemand.xml"
CONFIG_FILE="/proj/eta-automation/tapas/sessions/cil/washingmachine_ondemand-${TARGETHOST}.xml"

mkdir -p /proj/eta-automation/tapas/sessions/cil
cp $BASE_CONFIG_FILE $CONFIG_FILE
cp $CONFIG_FILE tapas-config.xml

echo ""
env
echo ""

logfile="tapasconsole.txt"
test -f $logfile && rm $logfile
touch $logfile

${TAPAS_BASE}/tapas_runner.py --define=__TARGETHOST__=${TARGETHOST} \
--define=__TARGETHOST2__=${TARGETHOST2} \
--define=__TARGETHOST3__=${TARGETHOST3} \
--define=__INSTALLNODE__="${INSTALLNODE}" \
--define=__MSV__=${MSV} -v -v -s $CONFIG_FILE >> $logfile 2>&1 &
tapaspid=$!

tail -f $logfile &
logpid=$!
wait $tapaspid
kill $logpid
