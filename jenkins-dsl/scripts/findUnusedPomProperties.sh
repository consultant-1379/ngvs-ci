# add git to PATH
PATH=/opt/local/dev_tools/git/latest/bin:${PATH}
export PATH

# search and find unused properties in each pom file
for file in `git diff-tree --no-commit-id --name-only -r ${GERRIT_PATCHSET_REVISION}`; do
  if [[ $file =~ .*pom.xml ]]; then
    while IFS=\> read -d \< entity content
      do
          tag_name=${entity%% *}
          if [[ $tag_name == "properties" ]]; then
            in=true
          elif [[ $tag_name == "/properties" ]]; then
            in=
          elif [[ "$in" && $tag_name != /* && $tag_name != "!--" && $tag_name == *version && ${content} == *SNAPSHOT* ]]; then
            count=$(grep -o "\${$tag_name}" $file | wc -l)
            if [[ $count == 0 ]]; then
              echo "Unused property: $tag_name" >> console_out.txt
            fi
          fi
    done < $file &> /dev/null
  fi
done

