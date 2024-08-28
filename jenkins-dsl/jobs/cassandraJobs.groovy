import javaposse.jobdsl.dsl.DslFactory

import com.ericsson.SyncJobBuilder
import com.ericsson.cassandra.CassandraEricssonJobBuilder
import com.ericsson.cassandra.CassandraGerritJobBuilder
import com.ericsson.cassandra.CassandraJobBuilder
import com.ericsson.cassandra.CreateEricssonBranchJobBuilder
import com.ericsson.cassandra.rpm.CassandraRpmJobBuilder
import com.ericsson.javadriver.JavaDriverEricssonJobBuilder
import com.ericsson.javadriver.JavaDriverGerritJobBuilder
import com.ericsson.javadriver.JavaDriverJobBuilder
import com.ericsson.javadriver.JavaDriverCreateEricssonBranchJobBuilder
import com.ericsson.AbstractCassandraJob

def proc = 'ssh -p 29418 gerritmirror.lmera.ericsson.se gerrit ls-projects'.execute() | 'grep cassandra/'.execute()

def projects = proc.text.split("\n")

folder('cassandra') {}
def folderNameCassandra = 'cassandra/cassandra'
def folderNameCassandraRpm = 'cassandra/cassandra-rpm'
def folderNameJavaDriver = 'cassandra/java-driver'

folder(folderNameCassandra) {}
folder(folderNameCassandraRpm) {}
folder(folderNameJavaDriver) {}

String customWSPath = "workspace/\${JOB_NAME}"

List cassandraBranchesForJdk8 = new ArrayList<String>()
List cassandraBranchesForJdk7 = new ArrayList<String>()
List javadriverBranchesForJdk8 = new ArrayList<String>()
List javadriverBranchesForJdk7 = new ArrayList<String>()

final String CREATE_ERICSSON_BRANCH_DESC = '<h2>A job to create an internal E/// branch</h2>\n' +
        '<p>Create an ericsson branch and create a commit with the needed changes for it to be built on our internal CI environment.<br/>\n' +
        'More info can be found in the <a href="https://openalm.lmera.ericsson.se/plugins/mediawiki/wiki/cassandra/index.php/BranchStrategy">' +
        'branch strategy</a> page at the Cassandra wiki.</p>'

projects.each {

    def repositoryName = it

    def getBranchesCmd = '/opt/local/dev_tools/git/latest/bin/git ls-remote -h ssh://gerritmirror.lmera.ericsson.se:29418/' + repositoryName + '.git'
    def branchesProc = getBranchesCmd.execute()

    def branches = branchesProc.in.text.readLines().collect {
        it.replaceAll(/[a-z0-9]*\trefs\/heads\//, '')
    }

    branches.each {
        def branch = it

        def branchName = branch
        if (branch.contains("/")) {
            branchName = branch.replaceAll('/', '_')
        }

        String jdk = AbstractCassandraJob.DEFAULT_JDK

        if (repositoryName.equals("cassandra/cassandra")) {

            if (isValidCassandraBranch(branch) && isJava8CassandraBranch(branch)) {
                jdk = AbstractCassandraJob.JAVA8_JDK
                cassandraBranchesForJdk8.add(branch)
            }
            else if (isValidCassandraBranch(branch)) {
                cassandraBranchesForJdk7.add(branch)
            }

            if (branch.contains("cassandra-")) {
                new CassandraJobBuilder(
                        name: folderNameCassandra + "/" + branchName + '_deploy',
                        description: 'Build and deploy from branch ' + branch,
                        gitUrl: 'ssh://gerritmirror.lmera.ericsson.se:29418/cassandra/cassandra.git',
                        branch: 'origin/' + branch,
                        jdk: jdk,
                        workspace: customWSPath
                        ).build(this as DslFactory)
            }
            else if (branch.contains("ericsson-")){
                new CassandraEricssonJobBuilder(
                        name: folderNameCassandra + "/" + branchName + '_deploy',
                        description: 'Build and deploy from branch ' + branch,
                        gitUrl: 'ssh://gerritmirror.lmera.ericsson.se:29418/cassandra/cassandra.git',
                        branch: 'origin/' + branch,
                        jdk: jdk,
                        workspace: customWSPath
                        ).build(this as DslFactory)
            }
            else if (branch.contains("cil-")){
                new CassandraEricssonJobBuilder(
                        name: folderNameCassandra + "/" + branchName + '_deploy',
                        description: 'Build and deploy CIL specific version from branch ' + branch,
                        gitUrl: 'ssh://gerritmirror.lmera.ericsson.se:29418/cassandra/cassandra.git',
                        branch: 'origin/' + branch,
                        jdk: jdk,
                        workspace: customWSPath
                        ).build(this as DslFactory)
            }
            else if (branch.contains("vs-")){
                new CassandraEricssonJobBuilder(
                        name: folderNameCassandra + "/" + branchName + '_deploy',
                        description: 'Build and deploy VS specific version from branch ' + branch,
                        gitUrl: 'ssh://gerritmirror.lmera.ericsson.se:29418/cassandra/cassandra.git',
                        branch: 'origin/' + branch,
                        jdk: jdk,
                        workspace: customWSPath
                        ).build(this as DslFactory)
            }
            else if (branch.equals("trunk")){
                new CassandraJobBuilder(
                        name: folderNameCassandra + '/' + branchName + '_deploy',
                        description: 'Build and deploy from branch ' + branch,
                        gitUrl: 'ssh://gerritmirror.lmera.ericsson.se:29418/cassandra/cassandra.git',
                        branch: 'origin/' + branch,
                        jdk: jdk,
                        workspace: customWSPath
                        ).build(this as DslFactory)
            }
        }
        else if (repositoryName.equals("cassandra/java-driver")) {

            if (isJava8JavaDriverBranch(branch)) {
                jdk = AbstractCassandraJob.JAVA8_JDK
                javadriverBranchesForJdk8.add(branch)
            }
            else if (isJava7JavaDriverBranch(branch)) {
                javadriverBranchesForJdk7.add(branch)
            }

            if (branch.equals("2.1")) {
                new JavaDriverJobBuilder(
                        name: folderNameJavaDriver + "/" + branchName + '_deploy',
                        description: 'Build and deploy from branch ' + branch,
                        gitUrl: 'ssh://gerritmirror.lmera.ericsson.se:29418/cassandra/java-driver.git',
                        branch: 'origin/' + branch,
                        jdk: jdk,
                        workspace: customWSPath
                        ).build(this as DslFactory)
            }
            else if (branch.contains("ericsson-")) {
                new JavaDriverEricssonJobBuilder(
                        name: folderNameJavaDriver + "/" + branchName + '_deploy',
                        description: 'Build and deploy from branch ' + branch,
                        gitUrl: 'ssh://gerritmirror.lmera.ericsson.se:29418/cassandra/java-driver.git',
                        branch: 'origin/' + branch,
                        jdk: jdk,
                        workspace: customWSPath
                        ).build(this as DslFactory)
            }
        }
        else if (repositoryName.equals("cassandra/cassandra-rpm")) {
            new CassandraRpmJobBuilder(
                    name: folderNameCassandraRpm + "/" + branchName + '_deploy',
                    description: 'Build and deploy from branch ' + branch,
                    gitUrl: 'ssh://gerritmirror.lmera.ericsson.se:29418/cassandra/cassandra-rpm.git',
                    branch: 'origin/' + branch,
                    workspace: customWSPath
                    ).build(this as DslFactory)
        }
    }
}

new SyncJobBuilder(
        name: folderNameCassandra + '/cassandra_sync',
        syncScriptFile: readFileFromWorkspace('scripts/cassandra_sync.sh'),
        description: 'Job synchronize the cassandra repository from git://git.apache.org/cassandra.git ' +
                '(if not up then https://github.com/apache/cassandra.git) to ssh://gerrit.ericsson.se:29418/cassandra/cassandra',
        workspace: customWSPath
        ).build(this as DslFactory)

new SyncJobBuilder(
        name: folderNameJavaDriver + '/java-driver_sync',
        syncScriptFile: readFileFromWorkspace('scripts/javadriver_sync.sh'),
        description: 'Job synchronize the java-driver repository from https://github.com/datastax/java-driver.git to ' +
                'ssh://gerrit.ericsson.se:29418/cassandra/java-driver.git',
        workspace: customWSPath
        ).build(this as DslFactory)

new CreateEricssonBranchJobBuilder(
        name: folderNameCassandra + '/__create_new_ericsson_branch',
        description: CREATE_ERICSSON_BRANCH_DESC,
        gitUrl: 'ssh://gerritmirror.lmera.ericsson.se:29418/cassandra/cassandra.git',
        branch: 'origin/trunk',
        workspace: customWSPath
        ).build(this as DslFactory)

new CassandraGerritJobBuilder(
        name: folderNameCassandra + '/cassandra-gerrit_unit_test-jdk7',
        workspace: customWSPath,
        branchesToBeTriggered: cassandraBranchesForJdk7
        ).build(this as DslFactory)

new CassandraGerritJobBuilder(
        name: folderNameCassandra + '/cassandra-gerrit_unit_test-jdk8',
        workspace: customWSPath,
        jdk: AbstractCassandraJob.JAVA8_JDK,
        branchesToBeTriggered: cassandraBranchesForJdk8
        ).build(this as DslFactory)

new JavaDriverGerritJobBuilder(
        name: folderNameJavaDriver + '/java-driver-gerrit_unit_test-jdk7',
        workspace: customWSPath,
        branchesToBeTriggered: javadriverBranchesForJdk7
        ).build(this as DslFactory)

new JavaDriverGerritJobBuilder(
        name: folderNameJavaDriver + '/java-driver-gerrit_unit_test-jdk8',
        workspace: customWSPath,
        jdk: AbstractCassandraJob.JAVA8_JDK,
        branchesToBeTriggered: javadriverBranchesForJdk8
        ).build(this as DslFactory)

new JavaDriverCreateEricssonBranchJobBuilder(
        name: folderNameJavaDriver + '/__create_new_ericsson_branch',
        description: CREATE_ERICSSON_BRANCH_DESC,
        gitUrl: 'ssh://gerritmirror.lmera.ericsson.se:29418/cassandra/java-driver.git',
        workspace: customWSPath
        ).build(this as DslFactory)

boolean isJava8CassandraBranch(String branch) {
    return branch.contains("cassandra-3") || branch.contains("ericsson-3") ||
            branch.contains("cil-3") || branch.contains("vs-3") ||
            branch.contains("trunk") || branch.contains("ericsson-trunk")
}

boolean isValidCassandraBranch(String branch) {
    return branch.contains("cassandra-") || branch.contains("ericsson-") ||
            branch.contains("cil-") || branch.contains("vs-") ||
            branch.contains("trunk")
}

boolean isJava8JavaDriverBranch(String branch) {
    return branch.contains("ericsson-3")
}

boolean isJava7JavaDriverBranch(String branch) {
    return !isJava8JavaDriverBranch(branch) && (branch.startsWith("2.1") || branch.contains("ericsson-"))
}