#!/bin/bash
set -e

#TODO: chargingcore is hardcoded here
readonly LOCKFILE_DIR=/proj/eta-automation/jenkins/kascmadm/clusters/rmca/locks
readonly CLUSTER_CONFIG_DIR=/proj/eta-automation/jenkins/kascmadm/clusters/rmca/config
readonly CLUSTER_STATUS_DIR=/proj/eta-automation/jenkins/kascmadm/clusters/rmca/status
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

    for srcdir in jivetest restsimulator pom.xml testdata compile; do
        if test -e ${srcdir} && ! git diff --quiet HEAD~1 ${srcdir}; then
            export DOWNLOAD_JIVE_TESTS=false
            break
        fi
    done

    for srcdir in selenium restsimulator pom.xml testdata compile; do
        if test -e ${srcdir} && ! git diff --quiet HEAD~1 ${srcdir}; then
            export DOWNLOAD_JIVESELENIUM_TESTS=false
            break
        fi
    done

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
    export BASE_CONFIG_FILE="/proj/eta-automation/tapas/config/rmca/suites/gerrit_jive_test.xml"

    export BUILD_USER_ID=`ssh -p 29418 gerrit.epk.ericsson.se gerrit query --format=TEXT ${GERRIT_PATCHSET_REVISION} limit:1 | grep -Po '(?<=username: ).*'`

    CONFIG_FILE="/proj/eta-automation/tapas/sessions/rmca/gerrit_jive_test-${targethost}.xml"
    cp ${BASE_CONFIG_FILE} ${CONFIG_FILE}

    echo ""
    env | sort
    echo ""

    logfile="tapasconsole.txt"
    test -f ${logfile} && rm ${logfile}
    touch ${logfile}

    if [ -z "${DOWNLOAD_JIVE_TESTS}" ]; then
      DOWNLOAD_JIVE_TESTS=true
    fi
    if [ -z "${DOWNLOAD_JIVESELENIUM_TESTS}" ]; then
      DOWNLOAD_JIVESELENIUM_TESTS=true
    fi

    EXECUTION_HOST_PORT=$((${ALLOCATED_PORT}+1))

    ${TAPAS_BASE}/tapas_runner.py \
    --define=__MSV__=${msvhost} \
    --define=__TARGETHOST__=${targethost} \
    --define=__CIL__=${cilhost} \
    --define=__RUN_JIVE_TESTS__=true \
    --define=__DOWNLOAD_JIVE_TESTS__=${DOWNLOAD_JIVE_TESTS} \
    --define=__DOWNLOAD_JIVESELENIUM_TESTS__=${DOWNLOAD_JIVESELENIUM_TESTS} \
    --define=__WITH_GERRIT_FEEDBACK__=${WITH_GERRIT_FEEDBACK} \
    --define=__PROJECT__=RMCA \
    --define=__INSTANCE__=${INSTANCE} \
    --define=__DISPLAYHOME__=${WORKSPACE} \
    --define=__EXECUTION_HOST_PORT__=${EXECUTION_HOST_PORT} \
    --define=__MAVEN_SETTINGS__=${MAVEN_SETTINGS} \
    --define=__MAVEN_REPOSITORY__=${MAVEN_REPOSITORY} \
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

    cilhost=`/bin/cat ${CLUSTER_CONFIG_DIR}/${cluster}.json | python -c 'import json,sys;obj=json.load(sys.stdin);print obj["'${cluster}'"]["cil"]'`
    echo "cil: $cilhost"
    export cilhost=${cilhost}

    targethost=`/bin/cat ${CLUSTER_CONFIG_DIR}/${cluster}.json | python -c 'import json,sys;obj=json.load(sys.stdin);print obj["'${cluster}'"]["targethost"]'`
    export targethost=${targethost}
    echo "targethost: $targethost"

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
        exit 2
    fi

    if [ ! -f "$CLUSTER_CONFIG_DIR/$CLUSTER_USED.json" ]; then
        echo "File not found: $CLUSTER_CONFIG_DIR/$CLUSTER_USED.json"
        exit 1
    fi

    # Find jive url
    jive_url_cmd="grep 'Jive session id: ' $WORKSPACE/tapasconsole.txt | gawk -F'id: ' '{print \$2;exit;}'"
    jive_web_session=$(bash -c "$jive_url_cmd")
    if [ -z "$jive_web_session" ]; then
        jive_web_session="latest"
    fi
    #jive_web_url="https://jive.epk.ericsson.se/#/projects/rmca/session-groups/session-group/result?context=GerritReview&suite=JiveSuite&session-user=kascmadm&test-results-filter=%7B%7D&test-results-session-id=$jive_web_session"
    #jive_selenium_web_url="https://jive.epk.ericsson.se/#/projects/rmca/session-groups/session-group/result?context=GerritReview&suite=RmcaSeleniumSmokeSuite&session-user=kascmadm&test-results-filter=%7B%7D&test-results-session-id=$jive_web_session"
    jive_web_url=$(grep 'RmcaSuite: ' $WORKSPACE/tapasconsole.txt| gawk -F'RmcaSuite: ' '{print $2;exit;}')
    jive_selenium_web_url=$(grep 'RmcaSeleniumSmokeSuite: ' $WORKSPACE/tapasconsole.txt | gawk -F'RmcaSeleniumSmokeSuite: ' '{print $2;exit;}')
    echo "jive_web_url: $jive_web_url"
    echo "jive_selenium_web_url: $jive_selenium_web_url"

    jive_results=$(grep -A 1 'RmcaSuite: ' $WORKSPACE/tapasconsole.txt | tail -n 1 | gawk -F'run: ' '{print "Executed: "$2;exit;}')
    selenium_results=$(grep -A 1 'RmcaSeleniumSmokeSuite: ' $WORKSPACE/tapasconsole.txt | tail -n 1 | gawk -F'run: ' '{print "Executed: "$2;exit;}')

    tapas_url_cmd="grep 'Web url:' $WORKSPACE/tapasconsole.txt | gawk -F' ' '{print \$6;exit;}'"
    tapas_web_url=$(bash -c "$tapas_url_cmd")
    if [ -z "$tapas_web_url" ]; then
      tapas_web_url="https://tapas.epk.ericsson.se/#/suites/RMCA/Rmca%20Gerrit%20Jive%20Test"
    fi
    echo "tapas_web_url: $tapas_web_url"
    echo "JOB_DESCRIPTION JiveTest <a href=\"$jive_web_url\">$jive_results</a> <br> SeleniumTest <a href=\"$jive_selenium_web_url\">$selenium_results</a> <br> Tapas <a href=\"$tapas_web_url\">URL</a>, (${CLUSTER_USED})"

    echo "logpid: $TAPASLOG_PID, tapaspid: $TAPAS_PID"
    if test -n "$TAPASLOG_PID" ; then kill ${TAPASLOG_PID} ; fi;
}
trap on_exit INT QUIT TERM HUP PIPE 0

main
