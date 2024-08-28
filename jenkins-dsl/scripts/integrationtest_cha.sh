#!/bin/bash
echo "Fetching snapshot version.."
version=$(cat $PROJECT_NAME/pom.xml | grep "\([0-9]\+\.\)\+[0-9]-SNAPSHOT" | sed "s/\s*<.*>\(.*\)<.*/\1/")
echo $version
folder_name=$(echo $TARGET_GERRIT_NAME | sed "s/.*\///")

tpg=$(echo $TARGET_GERRIT_NAME | sed "s/.*\.\([a-z]*$\)/\1/")
mvn_settings=$(echo /proj/eta-automation/maven/kascmadm-settings_arm-$tpg.xml)

echo "Clone chacore.."
mkdir $folder_name ; cd $folder_name
git clone ssh://$GERRIT_USER@$GERRIT_SERVER:29418/$TARGET_GERRIT_NAME.core.bundle --reference=${GIT_CLONE_CACHE}
git clone ssh://$GERRIT_USER@$GERRIT_SERVER:29418/$TARGET_GERRIT_NAME.dataaccess --reference=${GIT_CLONE_CACHE}
git clone ssh://$GERRIT_USER@$GERRIT_SERVER:29418/$TARGET_GERRIT_NAME.integrationtest --reference=${GIT_CLONE_CACHE}

echo "Updating pom.."
cd com.ericsson.bss.rm.charging.core.bundle/
sed -i "s/\(<$PROJECT_NAME\.version>\).*\(<\/$PROJECT_NAME\.version>\)/\1$version\2/g" bom/pom.xml
echo "Building bundle.."
mvn clean install --settings $mvn_settings -Dorg.ops4j.pax.url.mvn.settings=$mvn_settings -Dmaven.repo.local=${MAVEN_REPOSITORY} -Dorg.ops4j.pax.url.mvn.localRepository=${MAVEN_REPOSITORY}

echo "Building dataaccess.."
cd ../com.ericsson.bss.rm.charging.dataaccess/
mvn clean install --settings $mvn_settings -Dorg.ops4j.pax.url.mvn.settings=$mvn_settings -Dmaven.repo.local=${MAVEN_REPOSITORY} -Dorg.ops4j.pax.url.mvn.localRepository=${MAVEN_REPOSITORY}

echo "Building integrationtest.."
cd ../com.ericsson.bss.rm.charging.integrationtest/
mvn clean install --settings $mvn_settings -Dorg.ops4j.pax.url.mvn.settings=$mvn_settings -Dmaven.repo.local=${MAVEN_REPOSITORY} -Dorg.ops4j.pax.url.mvn.localRepository=${MAVEN_REPOSITORY}
