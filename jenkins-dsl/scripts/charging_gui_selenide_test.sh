#!/bin/bash
HOST="vmx-cha006"

download_latest_rpm(){
    # Artifactory host
    server=https://arm.epk.ericsson.se/artifactory
    # Artifactory repo
    repo=proj-charging-dev

    # Maven artifact location
    groupid=com/ericsson/bss/rm/charging/gui
    artifactid=charging.gui
    path=$server/$repo/$groupid/$artifactid
    version=`curl -s $path/maven-metadata.xml | grep latest | sed "s/.*<latest>\([^<]*\)<\/latest>.*/\1/"`
    build=`curl -s $path/$version/maven-metadata.xml | grep '<value>' | head -1 | sed "s/.*<value>\([^<]*\)<\/value>.*/\1/"`
    rpmname=$artifactid-$build-rpm.rpm
    export rpmname=${rpmname}
    url=$path/$version/$rpmname

    # Download
    echo $url
    wget -q -N $url
}

install_rpm(){
    user="root"
    echo "echo EvaiKiO1" > $(pwd)/pass
    export SSH_ASKPASS="$(pwd)/pass"
    chmod +x $(pwd)/pass
    setsid scp ${rpmname} ${user}@${HOST}:/tmp/
    setsid ssh -o StrictHostKeyChecking=no ${user}@${HOST} << EOF
        if [ -f "/tmp/${rpmname}" ]; then
            rpm -iv --replacefiles /tmp/${rpmname}
            if [ $? != 0 ]; then
                exit 1
            fi
            if [ -z "$(ps -ef | grep org.jboss.Main)" ]; then
                ./usr/share/jbossas/bin/standalone.sh
            fi
        else
            exit 1
        fi
EOF
}

execute_selenide_testcases(){
    # Replace MSV_HOST with target host
    sed -i "s/%MSV_HOST%/${HOST}/g" ${WORKSPACE}/selenidetest/src/main/resources/portal.cfg
    # Comment portal.url=localhost line
    sed -i '/portal.url=localhost/s/^/# /' ${WORKSPACE}/selenidetest/src/main/resources/portal.cfg
    # Uncomment portal.url=https line
    sed -i 's/.*portal.url=https/portal.url=https/' ${WORKSPACE}/selenidetest/src/main/resources/portal.cfg
    # Sanity limit memory to 16Gb to protect from gconfd-2 memleak bug
    ulimit -v 16000000
    ulimit -a
    cd ${WORKSPACE}
    # Execute maven test
    mvn \
    clean test \
    -Dsurefire.useFile=false \
    -U \
    -B -e \
    -Djarsigner.skip=true \
    --settings ${MAVEN_SETTINGS} -Dorg.ops4j.pax.url.mvn.settings=${MAVEN_SETTINGS} \
    -Dmaven.repo.local=${MAVEN_REPOSITORY} -Dorg.ops4j.pax.url.mvn.localRepository=${MAVEN_REPOSITORY} \
    -DOSGI_PORT_OVERRIDE=${ALLOCATED_PORT}
}

main(){
    download_latest_rpm
    install_rpm
    execute_selenide_testcases
}

main
