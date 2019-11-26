#!/bin/bash
cd "$(dirname "$0")"
APP_ID="cpd-server"
if (java -jar "$APP_ID".jar list | grep -q "$APP_ID")
then
  echo "stopping $APP_ID..."
  java -jar "$APP_ID".jar stop "$APP_ID"
else
  echo "$APP_ID not found, is it running?"
fi
