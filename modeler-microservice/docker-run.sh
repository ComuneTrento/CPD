#!/bin/bash
cd "$(dirname "$0")"
docker pull gdimodica/cpd:cpd-remote-deploy
docker run -v "$PWD"/mongo-persistent:/var/lib/mongodb -v "$PWD"/config-persistent:/deploy/cpd-configuration  -p 8901:8901 -i -t gdimodica/cpd:cpd-remote-deploy /bin/bash
