#!/bin/bash

BASE_DIR=$(
  cd "$(dirname "$0")"
  pwd
)/..
export LOG_DIR=$BASE_DIR/logs

if [ $# -lt 1 ]; then
  echo "USAGE: $0 {start|stop|restart} [-fg]"
  exit 1
fi

COMMAND=$1
shift

if [ $COMMAND = "start" ]; then
  exec "$BASE_DIR/bin/auth-start.sh" -c bifromq.auth.service.AuthService -f config.yml "$@"
elif [ $COMMAND = "stop" ]; then
  exec "$BASE_DIR/bin/auth-stop.sh" bifromq.auth.service.AuthService
elif [ $COMMAND = "restart" ]; then
  sh "$BASE_DIR/bin/auth-stop.sh" bifromq.auth.service.AuthService
  "$BASE_DIR/bin/auth-start.sh" -c bifromq.auth.service.AuthService -f config.yml "$@"
fi