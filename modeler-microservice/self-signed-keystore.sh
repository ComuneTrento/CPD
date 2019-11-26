#!/usr/bin/env bash
if [ "$1" == "-h" ] || [ "$1" == "--help" ]; then
  echo
  echo "usage: ./self-signed-keystore.sh <alias> <password> <keyStoreFilename>"
  echo
  echo "if no parameters are supplied defaults to: <alias>=simpatico, <password>=simpatico, <keyStoreFilename>=keystore.jks"
  echo
  exit 0
fi
ALIAS=$1
if [ -z "$ALIAS" ]; then
  ALIAS=simpatico
fi
PASSWORD=$2
if [ -z "$PASSWORD" ]; then
  PASSWORD=simpatico
fi
FILENAME=$3
if [ -z "$FILENAME" ]; then
  FILENAME=keystore.jks
fi
echo "generating the keystore self-signed certificate..."
keytool -genkey -keyalg RSA -alias "$ALIAS" -storepass "$PASSWORD" -keystore "$FILENAME" -validity 1000 -keysize 2048
