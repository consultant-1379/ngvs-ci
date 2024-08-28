package com.ericsson.bss.job.invoicing

import com.ericsson.bss.job.UmiTestJobBuilder

class InvoicingUmiTestJobBuilder extends UmiTestJobBuilder {
    @Override
    protected void getOptionalInputParameters() {
        addVersionChoiceParam('CAUTILVERSION', 'https://arm.epk.ericsson.se/artifactory/proj-bssf-release-local/com/ericsson/bss/security/cautil',
                'Version of com.ericsson.bss.security.cautil to be used.')
        addVersionChoiceParam('CERTUTILVERSION', 'https://arm.epk.ericsson.se/artifactory/proj-bssf-release-local/com/ericsson/bss/security/certutil',
                'Version of com.ericsson.bss.security.certutil to be used.')
        addVersionChoiceParam('NSMSERVERVERSION', 'https://arm.epk.ericsson.se/artifactory/proj-bssf-release-local/com/ericsson/bss/security/nsm/nsmserver',
                'Version of com.ericsson.bss.security.nsm.nsmserver to be used.')
        addVersionChoiceParam('NSMAGENTVERSION', 'https://arm.epk.ericsson.se/artifactory/proj-bssf-release-local/com/ericsson/bss/security/nsm/nsmagent',
                'Version of com.ericsson.bss.security.nsm.nsmagent to be used.')
        addVersionChoiceParam('NSMADMINCLIENTVERSION',
                'https://arm.epk.ericsson.se/artifactory/proj-bssf-release-local/com/ericsson/bss/security/nsm/nsmadminclient',
                'Version of com.ericsson.bss.security.nsm.nsmadminclient to be used.')
        addVersionChoiceParam('TOMCATRPMVERSION', 'https://arm.epk.ericsson.se/artifactory/proj-bssf-release-local/com/ericsson/bss/security/nsm/tomcat-rpm',
                'Version of com.ericsson.bss.security.nsm.tomcat-rpm to be used.')
        List choiceList = ['true', 'false']
        addListChoiceParam('RUN_JIVE_TESTS', choiceList, 'If true, jive test included.')
    }

    @Override
    protected String getOptionalTapasParameters() {
        String params = '--define=__CAUTILVERSION__=\${CAUTILVERSION} \\\n'
        params += '--define=__CERTUTILVERSION__=\${CERTUTILVERSION} \\\n'
        params += '--define=__NSMSERVERVERSION__=\${NSMSERVERVERSION} \\\n'
        params += '--define=__NSMAGENTVERSION__=\${NSMAGENTVERSION} \\\n'
        params += '--define=__NSMADMINCLIENTVERSION__=\${NSMADMINCLIENTVERSION} \\\n'
        params += '--define=__TOMCATRPMVERSION__=\${TOMCATRPMVERSION} \\\n'
        params += '--define=__RUN_JIVE_TESTS__=\${RUN_JIVE_TESTS} \\\n'

        return params
    }
}
