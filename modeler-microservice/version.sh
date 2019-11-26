#!/usr/bin/env bash
echo "copying new dependencies (if any) before version evaluation..."
mvn dependency:copy-dependencies -Dsilent=true
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "version is $VERSION"