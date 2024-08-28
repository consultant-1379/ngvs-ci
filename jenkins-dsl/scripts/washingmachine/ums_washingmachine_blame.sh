umask 2
PYTHON_EGG_CACHE=$WORKSPACE"/.python-eggs"
export PYTHON_EGG_CACHE
PYTHONUNBUFFERED=1
export PYTHONUNBUFFERED
set -e

export PYTHONPATH="/proj/env/tapas"

PYTHONUNBUFFERED=1
export PYTHONUNBUFFERED

TAPAS_CONFIG=/proj/eta-automation/config/kascmadm/tapas_production.ini
export TAPAS_CONFIG
TAPAS_BASE="/proj/env/tapas/tapas/bin"

BASE_CONFIG_FILE="/proj/eta-automation/tapas/config/ums/suites/washingmachine_blame.xml"
CONFIG_FILE="/proj/eta-automation/tapas/sessions/ums/washingmachine_blame_${BUILD_NUMBER}.xml"
BUILDDESC=`echo ${UPDATE}`
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

${TAPAS_BASE}/tapas_runner.py --define=__STATUS__=${STATUS} \
--define=__SEND_MAIL__=${SEND_MAIL} \
--define=__JENKINS_URL__="${JENKINS_URL}" \
--define=__TAPAS_URL__="${TAPAS_URL}" \
--define=__JIVE_URL__="${JIVE_URL}" \
--define=__DEFAULT_RECIPIENTS__=${DEFAULT_RECIPIENTS} \
--define=__BLAME_CONFIG_FILE__=${BLAME_CONFIG_FILE} \
--define=__CISCAT_RESULT__="${CISCAT_RESULT}" \
--define=__UPSTREAM_JOB__="${UPSTREAM_JOB}" -v -v -s $CONFIG_FILE >> $logfile 2>&1 &
tapaspid=$!

tail -f $logfile &
logpid=$!

wait $tapaspid
sleep 5
kill $logpid