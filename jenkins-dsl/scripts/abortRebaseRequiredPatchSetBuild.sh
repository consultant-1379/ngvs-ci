################################
# Check if commit needs rebase #
################################

cd ${WORKSPACE}
echo "Check if patchset needs rebase"
REMOTE_LATEST_SUBMITTED_HASH=`git ls-remote ssh://${GERRIT_HOST}:${GERRIT_PORT}/${GERRIT_PROJECT} -h refs/heads/${GERRIT_BRANCH} | awk '{print $1}'`
git -c gc.auto=10000 fetch ssh://${GERRIT_HOST}:${GERRIT_PORT}/${GERRIT_PROJECT}
LOCAL_LATEST_SUBMITTED_HASH=`git merge-base ${GERRIT_PATCHSET_REVISION} ${REMOTE_LATEST_SUBMITTED_HASH}`

if [ "${REMOTE_LATEST_SUBMITTED_HASH}" != "${LOCAL_LATEST_SUBMITTED_HASH}" ]; then
    echo "JOB_DESCRIPTION Build aborted due to rebase required"
    if [ -n "${WITH_GERRIT_FEEDBACK}" ]; then
        notify=""
        if [ "${GERRIT_HOST}" != "gerritforge.lmera.ericsson.se" ]; then
            notify="--notify OWNER "
        fi
        ssh -o BatchMode=yes -p ${GERRIT_PORT} -l ${USER} ${GERRIT_HOST} \
            "gerrit review --project ${GERRIT_PROJECT} " \
            "$notify" \
            "-m \"${JOB_NAME} aborted due to patchset not rebased.\"" \
            "--verified -1 ${GERRIT_PATCHSET_REVISION}"
    fi
    exit 1
fi
echo "Patchset does not require rebase"
exit 0