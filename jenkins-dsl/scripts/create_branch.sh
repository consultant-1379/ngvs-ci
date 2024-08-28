###########################
#  Create release branch  #
###########################
if [ ! -z ${GIT_TAG} ] ; then

  #Get last part from '-'
  arr=$(echo ${GIT_TAG} | tr "-" "\n")
  version=""
  for x in $arr
  do
      version=${x}
  done

  #Read parts of the version
  OIFS=$IFS
  IFS='.' read -ra VERSION_PART <<< "$version"
  IFS=$OIFS

  MAJOR_VERSION=${VERSION_PART[0]}
  MINOR_VERSION=${VERSION_PART[1]}
  PATCH_VERSION=${VERSION_PART[2]}

  NEXT_PATCH_VERSION=`expr ${PATCH_VERSION} + 1`

  release_branch_version=${MAJOR_VERSION}.${MINOR_VERSION}
  release_branch="release/${release_branch_version}"

  #TODO: check so the tag exist

  echo "Checkout tag ${GIT_TAG}"
  status=`git checkout ${GIT_TAG}`
  echo "--------------------------------------"

  #TODO: check so we not already has a branch named.

  echo "Creating branch (should removed last digit) ${release_branch}"
  echo "New version to be set ${MAJOR_VERSION}.${MINOR_VERSION}.${NEXT_PATCH_VERSION}"

  #TODO: add maven local repository parameters etc.
  mvn release:branch -DpushChanges=false -DbranchName=${release_branch} -DreleaseVersion=${release_branch_version}.${NEXT_PATCH_VERSION}-SNAPSHOT \
  -DupdateBranchVersions=true -DupdateWorkingCopyVersions=true \
  --settings ${MAVEN_SETTINGS} -Dorg.ops4j.pax.url.mvn.settings=${MAVEN_SETTINGS} \
  -Dmaven.repo.local=${MAVEN_REPOSITORY} -Dorg.ops4j.pax.url.mvn.localRepository=${MAVEN_REPOSITORY}

  echo "Checkout release branch"
  git checkout ${release_branch}

  echo "Push new branch to gerrit"
  git push origin ${release_branch}:refs/heads/${release_branch}
else

  echo "Could not find git tag: ${GIT_TAG}"
  echo "Rolling thumbs"
fi