#!/bin/bash
cd "$(dirname "$0")"
if [ ! -d log ]; then
    mkdir log
fi
#check if config files already exist

source environment.sh

if [ ! -f $PRODUCTIONFILE_BK ]; then
    #echo "configuration files from a previous setup have been found. Do you want to keep them?[Y/n]"
    #if [ "$keepit" = "n" ] ; then
        echo "running a new configuration procedure ......." 
        ./configure.sh
	if [ $? -eq 1 ]; then
		exit 0
	fi 
    #fi
    else
	echo "restoring configuration files from a previous setup ..........."
	cp $PRODUCTIONFILE_BK $PRODUCTIONFILE
	cp $JSONCONF1_BK $JSONCONF1	
	cp $JSONCONF2_BK $JSONCONF2
	cp $JSONCONF3_BK $JSONCONF3
	cp $JSONCONF4_BK $JSONCONF4
	cp $CLUSTERCONF_BK $CLUSTERCONF
	cp $INDEXHTMLEN_BK $INDEXHTMLEN
	cp $INDEXHTMLES_BK $INDEXHTMLES
	cp $INDEXHTMLIT_BK $INDEXHTMLIT
	cp $INDEXHTMLGL_BK $INDEXHTMLGL
	cp $KEYSTOREFILE_BK $KEYSTOREFILE
	cp $OAUTH2PROVIDERSFILE_BK $OAUTH2PROVIDERSFILE

fi	

APP_ID="cpd-server"
if (java -jar "$APP_ID".jar list | grep -q "$APP_ID")
then
  echo "$APP_ID is already running: use stop.sh and then start.sh to re-run"
else
    echo "starting $APP_ID..."
    java -jar "$APP_ID".jar start --vertx-id="$APP_ID" -Dvertx.hazelcast.config="./conf/cluster.xml" -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4j2LogDelegateFactory -cluster
fi
