#!/bin/bash
set -e

#TODO: chargingcore is hardcoded here
readonly LOCKFILE_DIR=/proj/eta-automation/jenkins/kascmadm/clusters/chargingcore/locks
readonly CLUSTER_CONFIG_DIR=/proj/eta-automation/jenkins/kascmadm/clusters/chargingcore/config
readonly CLUSTER_STATUS_DIR=/proj/eta-automation/jenkins/kascmadm/clusters/chargingcore/status
CLUSTER_DIR="${CLUSTER_CONFIG_DIR}/*.json"

# Set SKIP_DIRTY_CLUSTER=true to skip dirty clusters.
# Use SKIP on regtests. Do not set SKIP in cluster wipe/reinstall jobs

report_lack_of_free_cluster_in_gerrit() {
    ssh -o BatchMode=yes \
            -p 29418 \
            -l jenkins-jive-test \
            ${GERRIT_HOST} \
            gerrit review \
            --project ${GERRIT_PROJECT} \
            --notify OWNER \
            -m '"Jive tests were not performed. Did not find a free cluster: '${BUILD_URL}'"' \
            --verified 0 \
            ${GERRIT_PATCHSET_REVISION}
}

main() {
    # TODO: create tapas task of locking cluster
    minutes_to_retry=5
    RETRY_IN_SECONDS=$((60 * $minutes_to_retry))
    # Retry locking a cluster for $RETRY_IN_SECONDS seconds
    end=$((SECONDS+$RETRY_IN_SECONDS))
    while [ $SECONDS -lt ${end} ]; do
        for clusterfile in `ls -t ${CLUSTER_DIR}`
        do
            cluster=$(basename ${clusterfile})
            cluster="${cluster%.*}"
            export CLUSTER_DIRTY_FILE=${CLUSTER_STATUS_DIR}/${cluster}.dirty
            export CLUSTER_UPGRADE_FILE=${CLUSTER_STATUS_DIR}/${cluster}.upgrade
            if [ -f ${CLUSTER_DIRTY_FILE} -a "${SKIP_DIRTY_CLUSTER:-false}" == "true" ]; then
                echo "Cluster ${cluster} is marked as dirty, skipping"
                echo ""
                continue
            elif [ -f $CLUSTER_UPGRADE_FILE ]; then
                echo "Cluster ${cluster} is marked as being upgraded, skipping"
                echo ""
                continue
            fi
            lock "$cluster" || continue;
            export CLUSTER_USED=${cluster}
            run_on_cluster "$cluster"
            exit 0;
        done
        echo "Did not find a free cluster, $SECONDS/$RETRY_IN_SECONDS"
        echo ""
        echo ""
        sleep 1
    done
}

lock() {
    # TODO: create tapas task of locking cluster
    lock_fd=200
    cluster=$1
    lock_file=${LOCKFILE_DIR}/${cluster}.lock

    # Create lock file
    eval "exec ${lock_fd}>>${lock_file}"

    # Acquire the lock
    flock -n ${lock_fd}
    result=$?
    if [ ${result} -eq 0 ]; then
      echo "pid:$$ HOST:${HOST} BUILD_URL:${BUILD_URL}" > ${lock_file}
      echo "Acquired lock on ${cluster}"
    else
      echo "Could not acquire lock on ${cluster}"
      cat ${lock_file}
    fi

    echo ""
    return ${result}
}

run_on_cluster() {
    echo "##################################"
    echo "##### Running on $1 "
    echo "##################################"

    CLUSTER_RUN_STARTED=1
    get_cluster_hosts "$1"
    run_tapas
}

run_tapas() {
    umask 2
    export PYTHON_EGG_CACHE=$WORKSPACE"/.python-eggs"
    export PYTHONUNBUFFERED=1

    export PYTHONPATH="/proj/env/tapas"
    export PATH="/proj/env/jive/:$PATH"

    export TAPAS_CONFIG=/proj/eta-automation/config/kascmadm/tapas_production.ini
    export TAPAS_BASE="/proj/env/tapas/tapas/bin"
    export BASE_CONFIG_FILE="/proj/eta-automation/tapas/config/charging/suites/installnode/gerrit_jive_test.xml"

    #get the signum from the latest git commit
    export BUILD_USER_ID=`ssh -p 29418 gerrit.epk.ericsson.se gerrit query --format=TEXT ${GERRIT_PATCHSET_REVISION} limit:1 | grep -Po '(?<=username: ).*'`

    CONFIG_FILE="/proj/eta-automation/tapas/sessions/charging/gerrit_jive_test-${corehost}.xml"
    cp ${BASE_CONFIG_FILE} ${CONFIG_FILE}

    echo ""
    env | sort
    echo ""

    logfile="tapasconsole.txt"
    test -f ${logfile} && rm ${logfile}
    touch ${logfile}

    reponame=$(cat .git/FETCH_HEAD | sed 's/.*:29418\///' | sed 's/.*\.//' | sed 's/./\U&/' | sed 's/\// /')
    if [ -z "$reponame" ]; then
        echo "Did not find repository name"
        exit 1
    fi

    EXECUTION_HOST_PORT=$((${ALLOCATED_PORT}+1))

    ${TAPAS_BASE}/tapas_runner.py \
    --define=__MSV__=${msvhost} \
    --define=__CORE__=${corehost} \
    --define=__ACCESS__=${accesshost} \
    --define=__CIL__=${cilhost} \
    --define=__RUN_JIVE_TESTS__=true \
    --define=__WITH_GERRIT_FEEDBACK__=${WITH_GERRIT_FEEDBACK} \
    --define=__REPOSITORY__="${reponame}" \
    --define=__PROJECT__=Charging \
    --define=__INSTANCE__=${INSTANCE} \
    --define=__DOWNLOAD_JIVE_TESTS__=${DOWNLOAD_JIVE_TESTS} \
    --define=__EXECUTION_HOST_PORT__=${EXECUTION_HOST_PORT} \
    -v -v -s ${CONFIG_FILE} >> $logfile 2>&1 &
    tapaspid=$!
    export TAPAS_PID=${tapaspid}

    tail -f ${logfile} &
    logpid=$!
    export TAPASLOG_PID=${logpid}

    set +e
    wait ${tapaspid}
    exitcode=$?
    set -e

    exit $exitcode
}

get_cluster_hosts() {
    # TODO: create tapas task for read cluster config
    echo "get hosts: $1"
    cluster=$1

    if [ ! -f "$CLUSTER_CONFIG_DIR/$CLUSTER_USED.json" ]; then
        echo "File not found: $CLUSTER_CONFIG_DIR/$CLUSTER_USED.json"
        exit 1
    fi

    accesshost=`/bin/cat ${CLUSTER_CONFIG_DIR}/${cluster}.json | python -c 'import json,sys;obj=json.load(sys.stdin);print obj["'${cluster}'"]["access"]'`
    echo "access: $accesshost"
    export accesshost=${accesshost}

    cilhost=`/bin/cat ${CLUSTER_CONFIG_DIR}/${cluster}.json | python -c 'import json,sys;obj=json.load(sys.stdin);print obj["'${cluster}'"]["cil"]'`
    echo "cil: $cilhost"
    export cilhost=${cilhost}

    corehost=`/bin/cat ${CLUSTER_CONFIG_DIR}/${cluster}.json | python -c 'import json,sys;obj=json.load(sys.stdin);print obj["'${cluster}'"]["core"]'`
    export corehost=${corehost}
    echo "core: $corehost"

    msghost=`/bin/cat ${CLUSTER_CONFIG_DIR}/${cluster}.json | python -c 'import json,sys;obj=json.load(sys.stdin);print obj["'${cluster}'"]["msg"]'`
    export msghost=${msghost}
    echo "msg: $msghost"

    msvhost=`/bin/cat ${CLUSTER_CONFIG_DIR}/${cluster}.json | python -c 'import json,sys;obj=json.load(sys.stdin);print obj["'${cluster}'"]["msv"]'`
    export msvhost=${msvhost}
    echo "msv: $msvhost"
}

on_exit() {
    echo "Trapped, will always run"
    if [ -z "$CLUSTER_RUN_STARTED" ]; then
        report_lack_of_free_cluster_in_gerrit
        echo "Did not find a free cluster, failing..."
        echo "JOB_DESCRIPTION &nbsp;&nbsp; Did not find a free cluster."

        if [ -n "${WITH_GERRIT_FEEDBACK}" ]
        then
            echo "Retrigger patchset or rerun job to try to get another cluster."
            notify=""
            if [ "${GERRIT_HOST}" != "gerritforge.lmera.ericsson.se" ]; then
                notify="--notify OWNER "
            fi
            ssh -o BatchMode=yes -p 29418 -l ${USER} ${GERRIT_HOST} \
                "gerrit review --project ${GERRIT_PROJECT} " \
                "$notify" \
                "-m '\"Cluster not acquired. Retrigger patchset or rerun " \
                "job to try to get another cluster, see: '${BUILD_URL}'\" " \
                "--verified -1 ${GERRIT_PATCHSET_REVISION}"
        fi
        exit 2
    fi

    if [ ! -f "$CLUSTER_CONFIG_DIR/$CLUSTER_USED.json" ]; then
        echo "File not found: $CLUSTER_CONFIG_DIR/$CLUSTER_USED.json"
        exit 1
    fi

    # jive url
    jive_web_url=$(grep 'JiveSuite: ' $WORKSPACE/tapasconsole.txt | gawk -F'JiveSuite: ' '{print $2;exit;}')
    jive_results=$(grep -A 1 'JiveSuite: ' $WORKSPACE/tapasconsole.txt | tail -n 1 | gawk -F'run: ' '{print "Executed: "$2;exit;}')

    # jive selenium url
    jive_selenium_web_url=$(grep 'JiveGuiSuite: ' $WORKSPACE/tapasconsole.txt | gawk -F'JiveGuiSuite: ' '{print $2;exit;}')
    selenium_results=$(grep -A 1 'JiveGuiSuite: ' $WORKSPACE/tapasconsole.txt | tail -n 1 | gawk -F'run: ' '{print "Executed: "$2;exit;}')

    # tapas url
    tapas_web_url=$(grep 'Web url:' $WORKSPACE/tapasconsole.txt | gawk -F' ' '{print $7;exit;}')
    echo "tapas_web_url: $tapas_web_url"

    echo "JOB_DESCRIPTION JiveTest <a href=\"$jive_web_url\">$jive_results</a> <br> SeleniumTest <a href=\"$jive_selenium_web_url\">$selenium_results</a> <br> Tapas <a href=\"$tapas_web_url\">URL</a>, (${CLUSTER_USED})"
    echo "logpid: $TAPASLOG_PID, tapaspid: $TAPAS_PID"
    if test -n "$TAPASLOG_PID" ; then kill ${TAPASLOG_PID} ; fi;
}
trap on_exit INT QUIT TERM HUP PIPE 0

main
