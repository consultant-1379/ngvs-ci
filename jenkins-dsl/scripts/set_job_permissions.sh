PATH=/opt/local/dev_tools/git/latest/bin:${PATH}
export PATH
git init
test -d .jenkins_permission || git clone --reference /workarea/bss-f_gen/kascmadm/.gitclonecache ssh://gerrit.epk.ericsson.se:29418/tools/sde .jenkins_permission
cd .jenkins_permission
git clean -fdxq
git fetch
git reset --hard origin/master

PYTHON_UNBUFFERED=1
export PYTHON_UNBUFFERED
env | sort > env.txt
/usr/bin/time /proj/env/bin/python -m jenkins_permissions.sde_jenkins_update_permissions --workers 4 --config jenkins_permissions/configuration/${PERMISSION_FILE} --filter ${project_name}