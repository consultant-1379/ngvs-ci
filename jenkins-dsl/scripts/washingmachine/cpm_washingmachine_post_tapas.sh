################################
#  Junit Publisher workaround  #
################################
# workaround for no junit reports see ticket for more info https://eta.epk.ericsson.se/helpdesk/view.php?id=3523
#If there is a junit report.
nbr_of_junit_reports=`find . -type f -name **TEST-*.xml | wc -l`
#Then create a dummy report.
if [ $nbr_of_junit_reports -gt 0 ]; then
  echo "unit report found"
else
  echo "no unit report found, generating a dummy report";
  dummy_folder=target/surefire-reports/
  mkdir -p ${dummy_folder};
  dummy_report=${dummy_folder}/TEST-dummy.xml
  #content
  touch ${dummy_report}
  cat <<EOF >> ${dummy_report}
<?xml version="1.0" encoding="UTF-8"?>
<testsuite>
<testcase name="testDummy" classname="Dummy" time="0.0"/>
</testsuite>
EOF
fi
