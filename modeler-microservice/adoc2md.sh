#!/usr/bin/env bash
# NOTE: this script assumes pandoc and asciidoc are installed in your system
#       (sudo apt install pandoc asciidoc)
if [ -z "$1" ]; then
  echo "please provide an asciidoc file to convert"
  exit 1
fi

[[ "$1" =~ ^(.*/)*(.*\.)+(.*)?$ ]]
DIR=${BASH_REMATCH[1]}
NAME=${BASH_REMATCH[2]%?}
EXT=${BASH_REMATCH[3]}

if [ -n "$EXT" ]; then
  EXT=.$EXT
fi

echo "DIR: $DIR"
echo "NAME: $NAME"
echo "EXT: $EXT"

asciidoc -b docbook -o - "$1" | pandoc -f docbook -t markdown_strict -o "$DIR$NAME".md
