#!/usr/bin/env bash
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)