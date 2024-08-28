###############################
#  Generate build.properties  #
###############################

FILE=build.properties

echo "#Download dependencies from ARM"                                                                         >> ${FILE}
echo "artifact.remoteRepository.central:     https://arm.epk.ericsson.se/artifactory/proj-charging-dev"        >> ${FILE}
echo "artifact.remoteRepository.java.net2:   https://arm.epk.ericsson.se/artifactory/proj-charging-dev"        >> ${FILE}
echo "artifact.remoteRepository.apache:      https://arm.epk.ericsson.se/artifactory/proj-charging-dev"        >> ${FILE}
echo "artifact.remoteRepository.jclouds:     https://arm.epk.ericsson.se/artifactory/proj-charging-dev"        >> ${FILE}
echo "artifact.remoteRepository.oauth:       https://arm.epk.ericsson.se/artifactory/proj-charging-dev"        >> ${FILE}
echo ""                                                                                                        >> ${FILE}

echo "#Maven settings"                                                                                         >> ${FILE}
echo "maven-repository-id:                   arm"                                                              >> ${FILE}
echo "maven-repository-url:                  https://arm.epk.ericsson.se/artifactory/proj-cassandra-dev-local" >> ${FILE}
echo "maven.setting:                         ${MAVEN_SETTINGS}"                                                >> ${FILE}
echo "#maven local reposiotry to fix"                                                                          >> ${FILE}
echo ""                                                                                                        >> ${FILE}

echo "#SCM settings"                                                                                           >> ${FILE}
echo "scm.connection:                        scm:ssh://gerrit.ericsson.se:29418/cassandra/cassandra.git"       >> ${FILE}
echo "scm.developerConnection:               scm:ssh://gerrit.ericsson.se:29418/cassandra/cassandra.git"       >> ${FILE}
echo ""                                                                                                        >> ${FILE}

echo "#SCM settings"                                                                                           >> ${FILE}
echo "maven-ant-tasks.local:                 ${MAVEN_REPOSITORY}"                                              >> ${FILE}