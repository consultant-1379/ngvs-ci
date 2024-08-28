#!/bin/bash
set -e
set -x
set -u

###################
# Deploy snapshot #
###################

publishPackage()
{
    if [[ "$DRY_RUN" == "false" ]]; then
        npm publish --tag $NPM_TAG
    fi
}

# placeholder for pre-publish script (don't remove this comment)

NPM_TAG=$SNAPSHOT_TAG
npm-snapshot ${BUILD_NUMBER}
publishPackage
