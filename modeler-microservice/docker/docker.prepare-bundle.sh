##!/usr/bin/env bash
#!/usr/bin/env sh
source version.sh
cd "$(dirname "$0")"/..
echo "starting docker compilation ($VERSION)..."
#mvn -DskipTests -Drelax -gs /usr/share/maven/conf/settings-docker.xml package -P $PROFILE
mvn clean package -P docker
echo "copying target jar to deploy-bundle..."
cp target/cpd-server-$VERSION-fat.jar target/deploy-bundle/cpd-server.jar
chmod 754 target/deploy-bundle/*.sh
