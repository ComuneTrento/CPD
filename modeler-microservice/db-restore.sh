#!/usr/bin/env bash

DUMP_DIR="$1" || {
  echo "dump directory parameter is missing"
  exit 1
}

mongorestore --dir "$DUMP_DIR" --nsInclude 'cpd.*' --drop --gzip
