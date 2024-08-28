package com.ericsson.bss.job.charging

import com.ericsson.bss.job.GerritJiveClusterUpgradeJobBuilder

class ChargingGerritJiveClusterUpgradeJobBuilder extends GerritJiveClusterUpgradeJobBuilder {

    @Override
    protected String getProductFolder() {
        return "chargingcore"
    }

    @Override
    protected String getTpgSpecificParameters() {
        return "    TARGETHOST=`/bin/cat \${CLUSTER_FILE_USED} | python -c 'import json,sys;" +
                                                                              "obj=json.load(sys.stdin);" +
                                                                              "print obj[\"'\${CLUSTER_USED}'\"]" +
                                                                              "[\"core\"]'`\n" +
               "    echo \"TARGETHOST=\${TARGETHOST}\" >> env.properties\n" +
               "    echo \"CHARGINGCORE: \$TARGETHOST\"\n" +
               "\n" +
               "    TARGETHOST2=`/bin/cat \${CLUSTER_FILE_USED} | python -c 'import json,sys;" +
                                                                                "obj=json.load(sys.stdin);" +
                                                                                "print obj[\"'\${CLUSTER_USED}'\"]" +
                                                                                "[\"access\"]'`\n" +
               "    echo \"CHARGINGACCESS: \$TARGETHOST2\"\n" +
               "    echo \"TARGETHOST2=\${TARGETHOST2}\" >> env.properties\n" +
               "    echo \"VERSION2=LATEST\"  >> env.properties\n" +
               "\n" +
               "    echo \"TARGETHOST3=\" >> env.properties\n"

    }
}
