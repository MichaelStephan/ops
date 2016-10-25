#!/bin/bash

if [ $# -lt 2 ] ; then
  echo "Usage ./$0 --port <PORT> <DASHBOARD-NAME>"
  exit 255
fi

if [ $1 != "--port" ] ; then
  echo "Usage ./$0 -p|--port <PORT>"
  exit 255
fi

PORT=$2

docker run --rm -i -t -v `pwd`/etc:/etc/riemann -p ${PORT}:4567 riemann-dash $3
