#!/bin/bash
set -exu

##########################################
# Create patch branch for an NPM project #
##########################################

# create a name for the new branch
PATCH_BRANCH_VERSION=$(echo $GIT_TAG | sed 's/^v\([0-9]\+\.[0-9]\+\.\).*/\1x/')
PATCH_BRANCH_NAME="patch-"$PATCH_BRANCH_VERSION

git checkout $GIT_TAG
git checkout -b $PATCH_BRANCH_NAME
git push origin $PATCH_BRANCH_NAME

echo "Branch '$PATCH_BRANCH_NAME' was successfully created."

# initialize first commit
sed -i 's/\("version" *: *"[0-9]\+\.[0-9]\+\)\.[0-9]\+"/\1.1-SNAPSHOT"/' package.json
COMMIT_MESSAGE=$(echo $PATCH_BRANCH_VERSION | sed 's/\(.*\)\.x$/\1.1-SNAPSHOT/')
git commit -am "Initialize $(versionn -i)"
git push origin $PATCH_BRANCH_NAME:refs/heads/$PATCH_BRANCH_NAME

# push snapshot to ARM
NPM_TAG="SNAPSHOT_"$PATCH_BRANCH_VERSION
npm-snapshot ${BUILD_NUMBER}
npm cache clean     # to avoid error "Didn't get expected byte count"
npm publish --tag $NPM_TAG

