#!/usr/bin/env bash
NAME=$1
if [ -z "$NAME" ]; then
  echo "please provide a name for the dump"
  exit 1
fi

COLLECTIONS="dis extensions models properties schemas user.feedbacks users"

for COLLECTION in $COLLECTIONS; do
  mongodump --gzip --db=cpd --collection=$COLLECTION --out="dump/$NAME"
done
