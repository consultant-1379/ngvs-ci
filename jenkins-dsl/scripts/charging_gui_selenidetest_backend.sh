echo "Start selenidetest backend: "
nohup mvn clean test \
-f ${WORKSPACE}/testutils/configbackend.provisioner/pom.xml \
-DskipTests=false \
-Dtest=ProvisionConfigBackendTest \
-DgoToSleep=true \
-DuseFixedEquinoxPort=true \
-DuseFixedZookeeperPort=true \
-DequinoxPort=$((${ALLOCATED_PORT}+2)) \
-DzookeeperPort=$((${ALLOCATED_PORT}+1)) \
-Dsurefire.useFile=false \
-Dmaven.repo.local=${MAVEN_REPOSITORY} \
-Djava.io.tmpdir=${WORKSPACE} \
-Dorg.ops4j.pax.url.mvn.settings=${MAVEN_SETTINGS} \
-Dorg.ops4j.pax.url.mvn.localRepository=${MAVEN_REPOSITORY} &
