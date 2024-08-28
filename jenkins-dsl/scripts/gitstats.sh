# add git to PATH
PATH=/opt/local/dev_tools/git/latest/bin:${PATH}
export PATH

# remove old output
rm -rf ${WORKSPACE}/gitstats_out

# find repositories
repos=`find ${WORKSPACE} -type f -name "config" -exec dirname {} \;`

# generate gitstats
/usr/bin/time -v /proj/eta-tools/gitstats/bin/gitstats -c project_name="'${PROJECT_NAME}'" ${repos} ${WORKSPACE}/gitstats_out

cp ${WORKSPACE}/gitstats_out/authors.html ${WORKSPACE}/gitstats_out/authors_hidden.html

# remove author data
cd ${WORKSPACE}/gitstats_out/
echo "-- Remove authors --"
for r in `ls *.html`
  do 
    NEW_FILE=${r}'_bak'
    echo $NEW_FILE
    cp ${r} $NEW_FILE
   cat $NEW_FILE | grep -v authors.html > ${r}
  done

#echo "-- Cleanup --"
rm *.html_bak
rm authors.html