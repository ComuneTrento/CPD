#!/usr/bin/env bash
PROFILE=$1
if [ -z "$PROFILE" ]; then
  PROFILE=production
fi
if [ "$PROFILE" != "develop" ] && [ "$PROFILE" != "production" ]; then
  echo "unknown profile '$PROFILE', it should be 'develop' or 'production'"
  exit 1
fi;
source version.sh
cd "$(dirname '$0')"
echo "starting profile '$PROFILE' compilation of version '$VERSION'..."
mvn clean package -P $PROFILE
echo "copying target jar to deploy-bundle..."
cp target/cpd-server-$VERSION-fat.jar target/deploy-bundle/cpd-server.jar
chmod 754 target/deploy-bundle/*.sh
