################################
#  Sync cassandra repositories #
################################
test -f .git || git init

#remove all local tags, we need to get the new once if they where moved.
git tag -l | xargs git tag -d

#Should be changed to gerrit central repo instead of gitolite.
INTERNAL_REPO_URL=ssh://gerrit.ericsson.se:29418/cassandra/cassandra
INTERNAL_REPO_NAME=internal_repo

EXTERNAL_REPO_URL=git://git.apache.org/cassandra.git
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
else
  EXTERNAL_REPO_URL=https://github.com/apache/cassandra.git
  echo "reverting to ${EXTERNAL_REPO_URL}"
  git remote set-url ${EXTERNAL_REPO_NAME} ${EXTERNAL_REPO_URL}
  git remote set-url --push ${EXTERNAL_REPO_NAME} no-pushing
  git fetch ${EXTERNAL_REPO_NAME}
fi

#Fetch all tags as well (http://stackoverflow.com/a/1208223)
git fetch --tags ${EXTERNAL_REPO_NAME}

#Force update all branches
for i in `git branch -r | sed 's/^ *//' | grep ^${EXTERNAL_REPO_NAME}`; do
  BRANCH_NAME=`echo ${i} | sed "s/.*${EXTERNAL_REPO_NAME}\///g"`
  INTERNAL_REFS=refs/heads/${BRANCH_NAME}
  echo "Sync ${i} to ${INTERNAL_REFS}"
  git push ${INTERNAL_REPO_NAME} ${i}:${INTERNAL_REFS} #Should we use force push? or fail
done

#Push all tags. Force if some was moved.
git push -f ${INTERNAL_REPO_NAME} --tags