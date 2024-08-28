GERRIT_USER="%CODE_QUALITY_USER%"
VERIFY_COMMIT=$(cat trackIdVerify.txt)
SONAR_CONSOLE=$(cat console_out.txt)
notify=""
if [ "%GERRIT_SERVER%" != "gerritforge.lmera.ericsson.se" ]; then
    notify="--notify OWNER"
fi
if [[ "${VERIFY_COMMIT}" == "No issue found" ]] && [[ "${SONAR_CONSOLE}" != *"No new issue"* ]]; then
  if [ -f "target/sonar/issues-report/issues-report-light.html" ]; then
    ssh -o BatchMode=yes -p 29418 -l ${GERRIT_USER} %GERRIT_SERVER% gerrit review --project ${GERRIT_PROJECT} $notify -m '"See Sonar report for new issues related to this commit %SITE_PREVIEW% "'  -l Code-Review=-1 ${GERRIT_PATCHSET_REVISION}
  fi
elif [[ "${VERIFY_COMMIT}" != "No issue found" ]] && [[ "${SONAR_CONSOLE}" == *"No new issue"* ]]; then
  ssh -o BatchMode=yes -p 29418 -l ${GERRIT_USER} %GERRIT_SERVER% gerrit review --project ${GERRIT_PROJECT} $notify -m '"Tracking-Id issue:
 '${VERIFY_COMMIT}'"'  -l Code-Review=-1 ${GERRIT_PATCHSET_REVISION}
elif [[ "${VERIFY_COMMIT}" != "No issue found" ]] && [[ "${SONAR_CONSOLE}" != *"No new issue"* ]]; then
  if [ -f "target/sonar/issues-report/issues-report-light.html" ]; then
    ssh -o BatchMode=yes -p 29418 -l ${GERRIT_USER} %GERRIT_SERVER% gerrit review --project ${GERRIT_PROJECT} $notify -m '"Tracking-Id issue:
  '${VERIFY_COMMIT}'

  See Sonar report for new issues related to this commit
  %SITE_PREVIEW% "'  -l Code-Review=-1 ${GERRIT_PATCHSET_REVISION}
  fi
fi
