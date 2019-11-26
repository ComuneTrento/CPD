#!/bin/bash
cd "$(dirname "$0")"
if [ ! -d log ]; then
    mkdir log
fi
APP_ID="cpd-server"
if (java -jar "$APP_ID".jar list | grep -q "$APP_ID")
then
  echo "$APP_ID is already running: use stop.sh and then start.sh to re-run"
else
    echo "starting $APP_ID..."
    java -jar "$APP_ID".jar start --vertx-id="$APP_ID" -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4j2LogDelegateFactory -cluster
fi
