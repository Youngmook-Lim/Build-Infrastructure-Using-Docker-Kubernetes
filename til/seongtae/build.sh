#!/bin/sh

HOST_BIND_MOUNT="$1"
JOB_NAME="$2"
BUILD_PATH="$3"

docker stop build-env && docker rm build-env
docker run --name build-env --rm -u 1000 -v ${HOST_BIND_MOUNT}/${JOB_NAME}:/home/tmp/workspace/ openjdk:11 sh -c "cd /home/tmp/workspace/repo/${BUILD_PATH}; sh gradlew clean build 2> jenkins-build-error || {touch NORMAL_FAILURE; exit 1;}
