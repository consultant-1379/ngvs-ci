package com.ericsson

import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class AbstractCassandraJob {

    public static final MAVEN_SETTINGS = "/proj/eta-automation/maven/kascmadm-settings_arm-cassandra.xml"
    public static final DEFAULT_JDK = 'Latest JDK 1.7 64bit'
    public static final JAVA8_JDK = 'Latest JDK 1.8 64bit'
    public static final RESTRICT_LABEL_MESOS = 'Linux_redhat_6.2_x86_64_mesos'
    protected final static String CASSANDRA_TROUBLESHOOT = "\n<h4>Problems?</h4>\n" +
    "<ul>\n" +
    "  <li>For issues see " +
    "<a href='https://eta.epk.ericsson.se/wiki/index.php5/Cassandra_troubleshoot'>Cassandra troubleshooting guide</a>.</li>\n" +
    "</ul>"

    String name
    String description
    String gitUrl
    String branch
    String workspace

    /**
     * Parse the name of this job name
     *
     * Example:
     * Full project name: cassandra/cassandra/ericsson-2.2-fix_resource_leak_multi_repair_deploy
     * should trigger cassandra/cassandra-rpm/ericsson-2.2 job.
     *
     * The same should be done for cassandra-X.Y and project specific e.g. cil-X.Y as well
     *
     * @return The name of the cassandra-rpm job to trigger or empty string if not found
     */
    protected String getCassandraRPMJobToTrigger() {
        Pattern pattern = Pattern.compile('(cassandra|ericsson|cil|vs)-(\\d+\\.\\d+)')
        Matcher matcher = pattern.matcher(name)

        if (matcher.find()) {
            String branchName = matcher.group()
            String cassandraRpmFolder = 'cassandra/cassandra-rpm'

            return '/' + cassandraRpmFolder + '/' + branchName + '_deploy'
        }
        else {
            return ''
        }
    }
}
