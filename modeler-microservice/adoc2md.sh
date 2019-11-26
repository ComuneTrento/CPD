#!/usr/bin/env bash
# NOTE: this script assumes pandoc and asciidoc are installed in your system
#       (sudo apt install pandoc asciidoc)
FILE=$1
if [ -z "$FILE" ]; then
  echo "please provide an asciidoc file to convert"
  exit 1
fi

[[ "$FILE" =~ ^(.*\.)+(.*)$ ]]
NAME=${BASH_REMATCH[1]%?}
EXT=${BASH_REMATCH[2]}
#echo "NAME:" $NAME "EXT:" $EXT

asciidoc -b docbook -o - "$FILE" | pandoc -f docbook -t markdown_strict -o "$NAME.md"