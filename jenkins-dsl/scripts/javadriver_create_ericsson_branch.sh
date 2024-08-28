if [ ! -z ${GIT_TAG} ] ; then

  #checkout the tag
  git checkout ${GIT_TAG}

  #branch exist
  branch_exists=`git branch -r | grep origin/${NEW_BRANCH_NAME}` || echo "branch not found continue..."

  if [[ -z ${branch_exists} ]]; then

    #Override release plugin
    maven_release_plugin=\<build\>\\n\<pluginManagement\>\\n\<plugins\>\\n\<plugin\>\\n\<artifactId\>maven-release-plugin\<\\/artifactId\>\\n\<version\>2.1\<\\/version\>\\n\<configuration\>\\n\<mavenExecutorId\>forked-path\<\\/mavenExecutorId\>\\n\<useReleaseProfile\>false\<\\/useReleaseProfile\>\\n\<arguments\>\${arguments}\<\\/arguments\>\\n\<\\/configuration\>\\n\<\\/plugin\>\\n\<\\/plugins\>\\n\<\\/pluginManagement\>

    sed -i s/\<build\>/${maven_release_plugin}/g pom.xml

    #Change to local gerrit repository
    sed -i s/\<connection\>.*/\<connection\>scm:git:ssh:\\/\\/gerrit.ericsson.se:29418\\/cassandra\\/java-driver.git\<\\/connection\>/g pom.xml
    sed -i s/\<developerConnection\>.*/\<developerConnection\>scm:git:ssh:\\/\\/gerrit.ericsson.se:29418\\/cassandra\\/java-driver.git\<\\/developerConnection\>/g pom.xml

 else
    echo "Already exist"

    #where is the tag
    if [[ ! -z ${tag_exists} ]] ; then
      echo tag_exists ${tag_exists}

      tag_hash=`echo ${tag_exists} | grep -Eo '^[^ ]+'`
      git log ${tag_hash} --oneline --decorate -1
    fi

    #where is the branch
    if [[ ! -z ${branch_exists} ]] ; then
      echo branch_exists ${branch_exists}

      git log origin/${NEW_BRANCH_NAME} --oneline --graph --decorate -1
    fi

    exit 1
  fi
fi