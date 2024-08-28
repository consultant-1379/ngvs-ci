#!/bin/bash
set -e

readonly LOCKFILE_DIR=/proj/eta-automation/jenkins/kascmadm/clusters/$PRODUCT_FOLDER/locks
readonly CLUSTER_CONFIG_DIR=/proj/eta-automation/jenkins/kascmadm/clusters/$PRODUCT_FOLDER/config
readonly CLUSTER_STATUS_DIR=/proj/eta-automation/jenkins/kascmadm/clusters/$PRODUCT_FOLDER/status

# Set SKIP_DIRTY_CLUSTER=true to skip dirty clusters.
# Use SKIP on regtests. Do not set SKIP in cluster wipe/reinstall jobs

main() {
    export CLUSTER_UPGRADE_FILE=${CLUSTER_STATUS_DIR}/${CLUSTER}.upgrade
    lock || { echo "Lock failed, exiting"; exit 1; }
    date "+%F %T ${BUILD_TAG}" >> ${CLUSTER_UPGRADE_FILE}
    exit 0
}

lock() {
    # TODO: create tapas task of locking cluster
    lock_fd=200
    lock_file=${LOCKFILE_DIR}/${CLUSTER}.lock

    # Create lock file
    eval "exec ${lock_fd}>>${lock_file}"

    # Acquire the lock
    flock -n ${lock_fd}
    result=$?
    if [ ${result} -eq 0 ]; then
      echo "pid:$$ HOST:${HOST} BUILD_URL:${BUILD_URL}" > ${lock_file}
    fi

    return ${result}
}

main
