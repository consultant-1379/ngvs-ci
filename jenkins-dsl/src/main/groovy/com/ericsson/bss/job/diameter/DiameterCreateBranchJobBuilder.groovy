package com.ericsson.bss.job.diameter

import com.ericsson.bss.job.CreateBranchJobBuilder

public class DiameterCreateBranchJobBuilder extends CreateBranchJobBuilder {

    @Override
    protected String createBranchShell() {
        String createReleaseBranchScript = '#!/bin/bash\n' + super.createBranchShell()

        addProductPrefixParameter()

        createReleaseBranchScript = replaceBranchWithProductPrefix(createReleaseBranchScript)
        createReleaseBranchScript = replaceVersionWithProductPrefix(createReleaseBranchScript)

        return addDashToProductPrefix() + createReleaseBranchScript.replace('  mvn release:branch', getWorkaroundForDiameterRelease() + '  mvn release:branch')
    }

    private addProductPrefixParameter() {
        job.with {
            parameters {
                stringParam('PRODUCT_PREFIX' , '', 'Optional. If this is a product specific EP/Release branch. E.g. sdp, occ etc.<br/>' +
                        'The prefix will be added to the branch name and the pom version. Leave this empty for non product specific EP/Release branch.')
            }
        }
    }

    private String replaceBranchWithProductPrefix(String createReleaseBranchScript) {
        return createReleaseBranchScript.replace('release_branch="release/\${release_branch_version}"',
                'release_branch="release/\${release_branch_version}${PRODUCT_PREFIX}"')
    }

    private String replaceVersionWithProductPrefix(String createReleaseBranchScript) {
        return createReleaseBranchScript.replace('-DreleaseVersion=\${release_branch_version}.\${NEXT_PATCH_VERSION}-SNAPSHOT',
                '-DreleaseVersion=\${release_branch_version}.\${NEXT_PATCH_VERSION}\${PRODUCT_PREFIX}-SNAPSHOT')
    }

    private String addDashToProductPrefix() {
        return getShellCommentDescription('Add dash to PRODUCT_PREFIX') +
                'if [ ! -z ${PRODUCT_PREFIX} ] ; then\n' +
                '  PRODUCT_PREFIX="-${PRODUCT_PREFIX}"\n' +
                'fi\n\n'
    }

    /**
     * Able to choose to push a release to the repository
     *
     * To be able to create a release branch we will need to run the maven
     * release plugin with the branch goal[1]. Instead of pushing the changes
     * directly to the repository we will need to override the option by the
     * '-DpushChanges' parameter as an input to the command line.
     * If instead this options is set in the pom file it will not be possible
     * to override the value.
     *
     * [1]http://maven.apache.org/maven-release/maven-release-plugin/branch-mojo.html
     *
     * @return Command to remove line
     */
    private String getWorkaroundForDiameterRelease() {
        return '  #Workaround to remove pushChanges in release configuration in pom.xml \n' +
                '  sed -i /\\<pushChanges\\>true\\<\\\\/pushChanges\\>/d pom.xml \n' +
                '  if git diff-index --quiet HEAD -- ; then echo "No changes" ; else git commit -am "Removed pushChanges from release" ; fi \n \n'
    }
}
