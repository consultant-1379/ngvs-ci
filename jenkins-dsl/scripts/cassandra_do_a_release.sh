#!/bin/sh
#################
#  Do a release #
#################
# If a release we will set the release tag and step to next development version.
#
# pre-requriments: Version in build.xml needs to be in format <version>-EABC
#

#Revert all manipulations of build.xml
git checkout -- build.xml

if [ ${RELEASE} == true ] ; then
  echo "Step to next development version"
  #Get the baseversion value
  version=`grep \"base.version\" build.xml | awk -F"\"" '{print $4}'`
  echo Version: ${version}

  #Set releas tag
  git tag "cassandra-${version}" HEAD

  #First part of the version e.g. X.Y.Z-E
  prefix_version=`echo ${version}| grep -o .*-E`
  echo Prefix: ${prefix_version}

  #Second part of the version e.g. 123
  version_number=`echo ${version}| grep -o '\-E.*' | grep -Eo [0-9]+`

  #Step to next version
  ((version_number++))

  #Padd zeros
  version_number=`printf "%03d\n" $version_number`
  echo Next version padded: ${version_number}

  #Step to new version in build.xml
  new_version=${prefix_version}${version_number}
  echo New version: ${new_version}

  version_tag="<property name=\"base.version\" value=\"${version}\"\/>"
  new_version_tag="<property name=\"base.version\" value=\"${new_version}\"\/>"

  echo Version tag: ${version_tag}
  echo New version tag : ${new_version_tag}

  #change version
  sed -i "s/${version_tag}/${new_version_tag}/g" build.xml

  git add build.xml

  git commit -m "Update versions for ${new_version}"

  #git log --decorate --oneline --graph --all --topo-order -25

  #Push new development version
  branch_to_push=`echo ${GIT_BRANCH} | sed s/origin//g`
  git push origin HEAD:refs/heads${branch_to_push}
  #Push release tag
  git push origin tag cassandra-${version}
fi
