##################################
#  Sync java-driver repositories #
##################################
test -f .git || git init

#Should be changed to gerrit central repo instead of gitolite.
INTERNAL_REPO_URL=ssh://gerrit.ericsson.se:29418/cassandra/java-driver
INTERNAL_REPO_NAME=internal_repo

EXTERNAL_REPO_URL=https://github.com/datastax/java-driver.git
EXTERNAL_REPO_NAME=external_sync_repo

#This may fail the build
#git remote rm ${EXTERNAL_REPO_NAME}

#Add remote to git config only fetch
if [[ `git remote -v | grep ${INTERNAL_REPO_NAME} | grep fetch` != *${INTERNAL_REPO_NAME}* ]] ; then
  git remote add ${INTERNAL_REPO_NAME} ${INTERNAL_REPO_URL}
#  git remote set-url --push ${INTERNAL_REPO_NAME} no-pushing
fi

#git fetch ${INTERNAL_REPO_NAME}

#Add remote to git config only fetch
if [[ `git remote -v | grep ${EXTERNAL_REPO_NAME} | grep fetch` != *${EXTERNAL_REPO_NAME}* ]] ; then
  git remote add ${EXTERNAL_REPO_NAME} ${EXTERNAL_REPO_URL}
  git remote set-url --push ${EXTERNAL_REPO_NAME} no-pushing
fi

#Fetch new changes
if `git fetch ${EXTERNAL_REPO_NAME}` ; then
  echo "Fetch from ${EXTERNAL_REPO_URL}"
fi

#Force update all branches
for i in `git branch -r | sed 's/^ *//' | grep ^${EXTERNAL_REPO_NAME}`; do
  BRANCH_NAME=`echo ${i} | sed "s/.*${EXTERNAL_REPO_NAME}\///g"`
  INTERNAL_REFS=refs/heads/${BRANCH_NAME}
  echo "Sync ${i} to ${INTERNAL_REFS}"
  git push -f ${INTERNAL_REPO_NAME} ${i}:${INTERNAL_REFS} #Should we use force push? or fail
done

#Push all tags.
git push ${INTERNAL_REPO_NAME} --tags