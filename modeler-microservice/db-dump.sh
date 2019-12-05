#!/usr/bin/env bash

if [ -z "$1" ]; then
  OUTPUT_DIRECTORY="dump/$(date +%Y%m%dT%H%M%SZ)"
else
  OUTPUT_DIRECTORY="$1"
fi

COLLECTIONS="dis extensions models properties schemas user.feedbacks users"

for COLLECTION in $COLLECTIONS; do
  mongodump --gzip --db="cpd" --collection="$COLLECTION" --out="$OUTPUT_DIRECTORY"
done
