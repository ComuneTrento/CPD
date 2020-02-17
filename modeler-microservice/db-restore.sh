#!/usr/bin/env bash

if [ -z "$1" ]; then
  echo "dump directory parameter is missing"
  exit 1
fi

DUMP_DIR="$1"

if [ -z "$2" ]; then
  DB_NAME="cpd"
else
  DB_NAME="$2"
fi

echo "DB_NAME: $DB_NAME"

mongorestore --dir "$DUMP_DIR" --nsInclude "$DB_NAME".* --drop --gzip
