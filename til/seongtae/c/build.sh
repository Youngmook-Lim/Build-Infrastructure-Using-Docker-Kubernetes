#!/bin/sh

HOST_BIND_MOUNT="$1"
JOB_NAME="$2"
BUILD_PATH="$3"

docker stop build-env && docker rm build-env
docker run --name build-env --rm -u 1000 -v ${HOST_BIND_MOUNT}/${JOB_NAME}:/home/tmp/workspace/ joykst96/build-env-cpp:1.0 sh -c "cd /home/tmp/workspace/repo/${BUILD_PATH}; mkdir build && cd build; cmake ..; make 2> jenkins-build-error || (touch NORMAL_FAILURE; exit 1;); rm CMakeCache.txt Makefile cmake_install.cmake; rm -rf CMakeFiles;"
