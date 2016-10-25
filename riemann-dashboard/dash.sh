#!/bin/bash

if [ $# -lt 1 ] ; then
  echo "Usage ./$0 <DASHBOARD NAME>"
  exit 255
fi

CONFIG="/etc/riemann/$1-config.rb"

if [ ! -e ${CONFIG} ] ; then
  echo "The config $CONFIG could not be found"
  exit 255
fi

riemann-dash /etc/riemann/$1-config.rb

