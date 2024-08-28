if [ ! -z ${GIT_TAG} ] ; then

  #chechout the tag
  git checkout ${GIT_TAG}

  #create local ericsson branch
  ericsson_name=`echo ${GIT_TAG} | sed s/cassandra/${BRANCH_PREFIX}/g`

  #branch exist
  branch_exists=`git branch -r | grep origin/${ericsson_name}` || echo "branch not found continue..."

  tag_exists=`git show-ref refs/tags/${ericsson_name}` || echo "tag not found continue..."

  if [[ -z ${branch_exists} && -z ${tag_exists} ]]; then

    #current version
    version=`grep \"base.version\" build.xml | awk -F"\"" '{print $4}'`
    echo Version: ${version}

    #append internal e/// strategy version numbering
    new_version=${version}-E000
    echo New version: ${new_version}

    version_tag="<property name=\"base.version\" value=\"${version}\"\/>"
    new_version_tag="<property name=\"base.version\" value=\"${new_version}\"\/>"

    echo Version tag: ${version_tag}
    echo New version tag : ${new_version_tag}

    #change version
    sed -i "s/${version_tag}/${new_version_tag}/g" build.xml

    git add build.xml
    git commit -m "Update versions for ${new_version}"

    git tag ${ericsson_name}

    #push tag and branch to gerrit
    git push origin tag ${ericsson_name}
    git push origin HEAD:refs/heads/${ericsson_name}

    #Next step in the process
    echo "The dsl job for cassandra must be triggered, and then a release should be done on the job cassandra_${ericsson_name}_deploy"
 else
    echo "Allready exist"

    #where is the tag
    if [[ ! -z ${tag_exists} ]] ; then
      echo tag_exists ${tag_exists}

      tag_hash=`echo ${tag_exists} | grep -Eo '^[^ ]+'`
      git log ${tag_hash} --oneline --decorate -1
    fi

    #where is the branch
    if [[ ! -z ${branch_exists} ]] ; then
      echo branch_exists ${branch_exists}

      git log origin/${ericsson_name} --oneline --graph --decorate -1
    fi

    exit 1
  fi
fi