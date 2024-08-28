package com.ericsson.bss.job

class PythonSonarRunnerJobBuilder extends SonarRunnerJobBuilder {
    public String coverageTestFilePath = "tests/run_tests.py"
    public String coverageOmitPaths = "env/*,*tests/*"

    @Override
    protected void initShellJobs(){
        shells.add(cleanUpWorkspaceMesos("\${WORKSPACE}"))
        shells.add(gitConfig("\${WORKSPACE}"))
        shells.add(createVirtualEnvDir())
        shells.add(getCoverageCommand())
        shells.add(cleanVirtualEnvDir())
    }

    protected String getCoverageCommand(){
        String mavenSubProjectCmd = ""
        if (mavenProjectLocation){
            mavenSubProjectCmd = mavenProjectLocation[0..-9] + "/"
        }
        String cmd = ""
        cmd += "##############\n" +
               "#  Coverage  #\n" +
               "##############\n"
        cmd += "# Install packages and coverage\n" +
                "export PATH=/opt/local/dev_tools/python/2.7.10/bin/:\$PATH\n" +
                "export PATH=/proj/eta-tools/mongodb/2.4.9/Linux_x86_64/mongodb-linux-x86_64-2.4" +
                ".9/bin/:\$PATH\n" +
                "export PYTHONPATH=\${WORKSPACE}/" + mavenSubProjectCmd+ "\n\n" +
                "cd \$VENV_TMP\n" +
                "virtualenv --system-site-packages env\n" +
                "source env/bin/activate\n" +
                "cd \${WORKSPACE}\n" +
                "pip install -r " + mavenSubProjectCmd + "pip_requirements.txt\n" +
                "pip install coverage\n\n" +
                "# Run the tests \n" +
                "python -m coverage run " + mavenSubProjectCmd + coverageTestFilePath + "  " +
                "--omit=" +
                coverageOmitPaths + "\n" +
                "python -m coverage html --omit=" + coverageOmitPaths + "\n" +
                "python -m coverage xml -i --omit=" + coverageOmitPaths
        return cmd
    }
}
