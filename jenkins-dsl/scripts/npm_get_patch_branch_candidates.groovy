def free_tags = []

String git_repo = "%GIT_URL%"

String existing_tags_cmd = "/opt/local/dev_tools/git/latest/bin/git ls-remote --tags " + git_repo +
        " | awk -F ' ' '{print \$2}' | grep -e '[0-9]\$' | sed 's/refs\\/tags\\/\\(.*\\)/\\1/' | sort -rV"

String existing_patch_branches_cmd = "/opt/local/dev_tools/git/latest/bin/git ls-remote --heads " + git_repo +
        " | awk -F ' ' '{print \$2}' | grep \"patch-\" | sed 's/refs\\/heads\\/patch-\\(.*\\)/v\\1/'"

def existing_tags = ['bash', '-c', existing_tags_cmd].execute().in.text
def existing_patch_branches = ['bash', '-c', existing_patch_branches_cmd].execute().in.text

existing_tags.tokenize('\n').each {
    if ( it[-1]  ==  '0' ) {
        free_tags.add(it)
    }
}

existing_patch_branches.tokenize('\n').each {
    String originalTag = it

    if ("x" == originalTag[-1]) {
        free_tags.remove( originalTag.replace("x", "0") )
    }
}

return free_tags
