#!/usr/bin/env bash
NAME=$1
if [ -z "$NAME" ]; then
  echo "please provide a name for the dump"
  exit 1
fi

COLLECTIONS="properties schemas models dis extensions"

for COLLECTION in $COLLECTIONS; do
  mongodump --gzip --db=cpd --collection=$COLLECTION --out="dump/$NAME"
done

SRC=$2
if [[ $NAME == "all" ]] && [[ $SRC ]] && [[ $SRC == "src" ]]; then
  echo "exporting jsons to src..."
  for COLLECTION in $COLLECTIONS; do
    mongoexport --db=cpd --collection=$COLLECTION --jsonArray --pretty --out src/main/deploy-bundle/data/db/collections/$COLLECTION.json
  done
fi