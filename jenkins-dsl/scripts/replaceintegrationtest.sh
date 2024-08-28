#!/bin/bash
git clone ssh://$GERRIT_USER@$GERRIT_SERVER:29418/$GERRIT_NAME $PROJECT_NAME ; cd $PROJECT_NAME
version=$(cat pom.xml | grep '[0-9]\+\.[0-9]\+\.[0-9]\+-SNAPSHOT' | sed -n 's/ *<version>\(.*\)<\/version>/\1/p')
echo $version
major_version=$(echo $version | sed -n 's/\([0-9]\+\).*/\1/p')
echo $major_version
tag_version=$(git tag | grep $major_version | tail -n 1 | sed -n 's/[A-Za-z]\+-//p')
echo $tag_version
git checkout $PROJECT_NAME-$tag_version
find integrationtest/ -type f -iname pom.xml -exec sed -i "s/$tag_version/$version/g" "{}" +;
echo "Release version replaced with snapshot in integrationtest poms"
echo "Replacing snapshot version of integrationtest.."
mv integrationtest/ ../
git checkout master
rm -rf integrationtest/
mv ../integrationtest/ .
echo "Replaced integrationtest. Building project.."
