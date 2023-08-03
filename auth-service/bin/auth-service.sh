#!/bin/bash

BASE_DIR=$(
  cd "$(dirname "$0")"
  pwd
)/..
export LOG_DIR=$BASE_DIR/logs/auth-service

if [ $# -lt 1 ]; then
  echo "USAGE: $0 {start|stop|restart} [-fg]"
  exit 1
fi

COMMAND=$1
shift

if [ $COMMAND = "start" ]; then
  exec "$BASE_DIR/bin/auth-start.sh" -c AuthService -f config.yml "$@"
elif [ $COMMAND = "stop" ]; then
  exec "$BASE_DIR/bin/auth-stop.sh" AuthService
elif [ $COMMAND = "restart" ]; then
  sh "$BASE_DIR/bin/auth-stop.sh" AuthService
  "$BASE_DIR/bin/auth-start.sh" -c AuthService -f config.yml "$@"
fi