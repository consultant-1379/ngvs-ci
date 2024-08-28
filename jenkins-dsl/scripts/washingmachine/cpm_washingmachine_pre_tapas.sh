#!/bin/ksh -e
##########################################
#  Prepare workspace for Jive execution  #
##########################################

rm -rf $WORKSPACE/temp $WORKSPACE/target
mkdir -p $WORKSPACE/temp

MAVEN_REPOSITORY=${WORKSPACE}/repository
mkdir -p ${MAVEN_REPOSITORY}/com/ericsson
find ${MAVEN_REPOSITORY}/com/ericsson -name '*-SNAPSHOT' -type d -print0 | xargs -0 rm -rf
find ${MAVEN_REPOSITORY} -type f -mmin +1440 -delete -o -name '*-SNAPSHOT' -type f -delete -o -name '*lastUpdated' -type f -delete -o -type d -empty -delete
