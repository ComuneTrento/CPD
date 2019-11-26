#!/usr/bin/env bash
NAME=$1
if [ -z "$NAME" ]
then
  NAMES=$(ls -1 dump | tr '\n' '|')
  echo "please provide the name of the dump (${NAMES%?})"
  exit 1
fi
if [ "$2" != "nodrop" ]
then
  DROP='--drop '
fi
mongorestore --dir "dump/$NAME" --nsInclude 'cpd.*' $DROP--gzip