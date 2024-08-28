#!/bin/bash
set -e
set -x
set -u

##################
# Deploy release #
##################

publishPackage()
{
    if [[ "$DRY_RUN" == "false" ]]; then
        npm publish --tag $NPM_TAG
    fi
}

pushCommit()
{
    if [[ "$DRY_RUN" == "false" ]]; then
        git push origin HEAD:refs/heads/$BRANCH_NAME
    fi
}

pushGitTag()
{
    if [[ "$DRY_RUN" == "false" ]]; then
        git push origin --tags
    fi
}

# increment package version and deploy a release
PACKAGE_VERSION=$(npm version ${RELEASE_TYPE} -m "[npm-release] Release %s")
# npm version creates a git tag as well
NPM_TAG=$RELEASE_TAG

# placeholder for pre-publish script (don't remove this comment)

pushCommit
pushGitTag
publishPackage

# placeholder for post-publish script (don't remove this comment)

# initialize new snapshot version
PACKAGE_VERSION=$(semver -i $RELEASE_STEP $PACKAGE_VERSION)"-SNAPSHOT"
sed -i 's/\("version" *: *"\)\(.*\)"/\1'$PACKAGE_VERSION'"/' package.json
git commit -am "Initialize $PACKAGE_VERSION"
pushCommit
