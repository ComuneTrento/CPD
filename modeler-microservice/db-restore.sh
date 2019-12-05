#!/usr/bin/env bash

DUMP_DIR="$1" || {
  echo "dump directory parameter is missing"
  exit 1
}

PARAMS='--gzip'

if [ "$2" == "drop" ]; then
  PARAMS="'--drop' $PARAMS"
fi
mongorestore --dir "$DUMP_DIR" --nsInclude 'cpd.*' "$PARAMS"
