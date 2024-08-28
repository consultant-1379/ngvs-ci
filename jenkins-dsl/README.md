# Description
The job-dsl-plugin allows the programmatic creation of projects using a DSL. Pushing job creation into a script allows you to automate and standardize your Jenkins installation, unlike anything possible before.

## Run Docker Container
### Login
To be able to download the image from the internal docker registry you need to login to the server.

```
$ docker login https://armdocker.rnd.ericsson.se/
```

**Note.** The docker run command should be run in the same folder as this jenkins dsl repository are cloned to. It will mount the code to the running container. So changes in repository will be applied to the running jenkins instance.

```
$ docker pull armdocker.rnd.ericsson.se/proj_eta/jenkins-dsl && docker run -t -i --rm=true \
-p 127.0.0.1:8080:8080 \
-p 50000:50000 \
-v `pwd`:/var/jenkins_home/jenkins-dsl \
--volume $SSH_AUTH_SOCK:/ssh-agent \
--env SSH_AUTH_SOCK=/ssh-agent \
armdocker.rnd.ericsson.se/proj_eta/jenkins-dsl \
/bin/bash -c "ssh-add -l; sed -i "s/USER/${USER}/g" /root/.ssh/config; /usr/local/bin/jenkins.sh"
```

Once Jenkins is up and running go to http://127.0.0.1:8080

##Run Jenkins on LVD
**Note.** If you are running Jenkins on your LVD, you also have to make it secure since it will be available through the company's network. You can find the manual for the Jenkins security configuration on the [official webpage](https://wiki.jenkins-ci.org/display/JENKINS/Securing+Jenkins). [Quick and Simple Security](https://wiki.jenkins-ci.org/display/JENKINS/Quick+and+Simple+Security) should be fine if you are the only person who is gonna use this Jenkins instance.

1. SSH to the LVD;
2. Create a Jenkins working directory:
```
mkdir -p /slask/$USER/jenkins/
```
3. Determine, which version of Jenkins you need to run;
4. Download Jenkins *.war file from the [Jenkins mirror](http://mirrors.jenkins.io/war-stable) and place it in ```/slask/$USER/jenkins/```;
5. In your scripts directory (for example: ```/home/$USER/scripts```) create a file called ```run_jenkins.sh``` with the following content:
```
#!/bin/sh
nohup java -DJENKINS_HOME="/slask/$USER/jenkins" -jar -XX:MaxPermSize=512m /slask/$USER/jenkins/jenkins.war > /slask/$USER/jenkins/nohup.out 2>&1 &
```
6. Make the file executable and start Jenkins by executing the file:
```
chmod +x /home/$USER/scripts/run_jenkins.sh
/home/$USER/scripts/run_jenkins.sh
```
7. Add it to your PATH if you want to be able to execute the script just by calling its name;
8. Jenkins now should be up and running on the following URL: ```<LVD>:8080``` ;
9. If Jenkins is not accessible by this URL, check the content of the ```/slask/$USER/jenkins/nohup.out``` file. It might contain the hints about what went wrong during the execution;
10. Copy plugins from any other Jenkins, where the plugins you need are already installed. Example:
```
scp -r kascmadm@sekaetalab2.epk.ericsson.se:/eta/jenkins-test/plugins /slask/$USER/jenkins/plugins
```
11. Put the DSL code in some directory in you home folder. For example in ```/home/$USER/repo/jenkins-dsl/```;
12. Copy the subfolder ```dsl_rm``` from the folder ```Examples``` from this repo to the folder ```/slask/$USER/jenkins/jobs/```;
13. If you placed jenkins-dsl code in a different directory, than ```/home/$USER/repo/jenkins-dsl/```, update it in ```dsl_rm/config.xml``` as well:
 find the line ```cp -r /home/$USER/repo/jenkins-dsl/* .``` and replace it with your custom location.
14. Open Jenkins Web App, go to ```Manage Jenkins -> Configure System``` and press ```Save```. This will create a variable ```JENKINS_URL``` (see http://jenkins-ci.361315.n4.nabble.com/Env-variable-for-JENKINS-URL-td3627015.html );
15. In the  Jenkins Web App, go to ```Manage Jenkins -> Reload Configuration from Disk```. After clicking on it you will have ```dsl_rm``` job on the main page. If the tpg you want to build is missing from the drop-down list, add it in the ```dsl_rm``` job configuration;
16. Configure [Jenkins security](https://wiki.jenkins-ci.org/display/JENKINS/Securing+Jenkins).
17. To check possible problems and solutions see FAQs;


## Restart or stop Jenkins
1. To restart Jenkins to to the following URL and perform restart:
```
<JENKINS_URL>/restart
```
2. To shut Jenkins down to to the following URL and perform shut down:
```
<JENKINS_URL>/exit
```


### FAQs
1. 'FATAL: [ERROR] not able to run ../git ls-remote'. Your docker container may not be able to reach machines at Ericsson DNS.
Verify so your DNS configuration /etc/default/docker. https://wiki.lmera.ericsson.se/wiki/Docker#dns_configuration

2. You are prompted with username and password when you pull the docker image.
You need to update your credentials, you can do this by ```$ docker login armdocker.rnd.ericsson.se```

3. 'java.lang.NullPointerException' occurs when you are running the 'dsl_rm' job while running Jenkins on LVD.
If you see the following logs make sure you performed the step with 'JENKINS_URL' from the section **Run Jenkins on LVD** above:
```
java.lang.NullPointerException
   at java.net.URLEncoder.encode(URLEncoder.java:205)
   at java_net_URLEncoder$encode.call(Unknown Source)
   at org.codehaus.groovy.runtime.callsite.CallSiteArray.defaultCall(CallSiteArray.java:42)
   at org.codehaus.groovy.runtime.callsite.AbstractCallSite.call(AbstractCallSite.java:108)
   at org.codehaus.groovy.runtime.callsite.AbstractCallSite.call(AbstractCallSite.java:120)
   at com.ericsson.bss.Project.getJenkinsEncodedUrl(Project.groovy:128)
   at com.ericsson.bss.Project.getWorkspacePath(Project.groovy:135)
   at com.ericsson.bss.Project.init(Project.groovy:119)
```

### Dockerfile
The docker file for dsl is located in https://gerrit.epk.ericsson.se/#/admin/projects/tools/eta/jenkins-dsl-docker
In the repository you find instructions how to build and deploy it in jenkins-dsl/README.md

# Steps to contribute:
1. Clone DSL code from [  gerrit DSL link](https://gerrit.epk.ericsson.se/#/admin/projects/tools/eta/jenkins-dsl).
2. Contribute the DSL code as you want.
3. Test your changes by using [Docker](#Run-Docker-Container) step.
4. Push your commit to gerrit for review, and add `ETA` gerrit group as reviewer.

# Digital BSS
## Add a new project
### Code
1. To create a new project a new project file needs to be created in <code>src/main/groovy/com/ericsson/bss/project/</code> .
1. The project file should extend Project.groovy
1. The method getRepositories must be override, specifying the repositories the jobs should be generated for.

### Jenkins
1. Create a new Freestyle job namned projectname_dsl.
1. Add 'Process Job DSLs'

    (X) Look on Filesystem
      DSL Scripts: jobs/bssJobs.groovy
    Advanced
      Additional classpath: src/main/groovy

#### Create Jobs as disabled
Since several jobs starts automatically on creation, or have a schedule, they can cause issues while testing the DSL. This can be avoided by setting the environment variable <code>ALL_DSL_JOBS_DISABLED</code> to true. This will ensure that no job is run accidentally during testing of DSL.

There are two ways to make this to work as intended:
##### System Environment Variable (should only be used on local/test Jenkins that can be shutdown easily)
1. <code>export ALL_DSL_JOBS_DISABLED=true</code>
1. Start jenkins
1. Run the "projectname_dsl" job and it will create all jobs as disabled
##### Jenkins Global Variable (should be used on live Jenkins)
1. In Jenkins -> Manage Jenkins -> Configure System
1. Under Global properties, mark "Environment variables" and add a variable:
    1. Name: <code>ALL_DSL_JOBS_DISABLED</code>
    1. Value: <code>true</code>
1. Run the "projectname_dsl" job and it will create all jobs as disabled

## Code structure
- <code>src/main/groovy/com/ericsson/bss/Project.groovy</code> Projects should extend this class.
- <code>src/main/groovy/com/ericsson/bss/project/</code> Init class for projects.
- <code>src/main/groovy/com/ericsson/bss/AbstractJobBuilder.groovy</code> JobBuilder classes should inherit.
- <code>src/main/groovy/com/ericsson/bss/job/{projectName}/</code> If needed to override some specific configuration for a default job, should be placed in seperate project package.
- <code>jobs/bssJobs.groovy</code> Class that should be triggered from Jenkins DSL job.

## FAQs
### + My changes are not applied in the production environemnt
You need to trigger the DSL job manually. No auto trigger because memory leak, more info in commit "0956b6bd Remove scm trigger for dsl job".

### + How do I add a new repository to the "multi release script"?
This is handled by a list in the ```createMultiRepositoryReleaseJob``` method that can be found in the project class. The project class can be found in the package ```src/main/groovy/com/ericsson/bss/project/```.

## Known issues
- Need to specify gerrit server to be used in job, if more than one server added to the jenkins server.
- Need to run plugins job-dsl v1.40+ and gradle v0.23+ and Jenkins v1.625.2+
- Do not start Jenkins in devinit environment

# Run Sonar on local machine:
- Download and install SonarQube: https://github.com/sonar-intellij-plugin/sonar-intellij-plugin
- Follow the steps to connect to ETA DSL rule base.
- Script to run incremental Sonar Analysis:
  gradle sonarqube -Dsonar.host.url=https://sonar.epk.ericsson.se -Dsonar.projectKey=com.ericsson.eta.jenkins.dsl \
  -Dsonar.projectName="ETA Jenkins dsl" -Dsonar.projectVersion=0.1 -Dsonar.sources=src/main/groovy,jobs -Dsonar.tests=src/test/groovy \
  -Dsonar.language=grvy -Dsonar.analysis.mode=incremental -Dsonar.profile=ETA -Dsonar.core.codeCoveragePlugin=jacoco

## Required plugins
Plugins can be found in plugins.txt in https://gerrit.epk.ericsson.se/#/admin/projects/tools/eta/jenkins-dsl-docker repository.
https://gerrit.epk.ericsson.se/plugins/gitiles/tools/eta/jenkins-dsl-docker/+/11636cc60fff3715c4ce76d345f0f33caa3e630b/jenkins-dsl/plugins.txt

## Links
- This project uses the [Jenkins API plugin](https://wiki.jenkins-ci.org/display/JENKINS/Job+DSL+Plugin) to generate Jenkins jobs.
- Link to [Jenkins DSL API](https://jenkinsci.github.io/job-dsl-plugin/)
- Dockerfile repository https://gerrit.epk.ericsson.se/#/admin/projects/tools/eta/jenkins-dsl-docker
