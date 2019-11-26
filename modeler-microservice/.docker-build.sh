#!/bin/bash
cd "$(dirname "$0")"
docker build -t cpd-develop -f Dockerfile.local.develop .
