#!/usr/bin/env bash
cd "$(dirname "$0")" || exit
USER=$1
if [ -z "$USER" ]
then
  echo "please provide ssh username"
  exit 1
fi
SERVER=$2
if [ -z "$SERVER" ]
then
  echo "please provide server domain or ip address ex. simpatico.mydomain.com"
  exit 1
fi
./prepare-bundle.sh production
APP_ID="cpd-server"
ssh $USER@$SERVER 'if [ ! -d "'$APP_ID'" ]; then echo "creating '$APP_ID' dir..."; mkdir "'$APP_ID'"; fi'
ssh $USER@$SERVER 'if (java -jar "'$APP_ID/$APP_ID'".jar list | grep -q "'$APP_ID'"); then echo "stopping remote server..."; ~/"'$APP_ID'"/stop.sh; fi'
echo "deploying to $SERVER..."
scp -r target/deploy-bundle/* $USER@$SERVER:"$APP_ID"/
echo "starting remote server..."
ssh $USER@$SERVER '~/"'$APP_ID'"/start.sh'
