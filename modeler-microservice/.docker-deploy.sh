#!/bin/bash
cd "$(dirname "$0")"
docker build -t cpd-deploy -f Dockerfile.deploy .
