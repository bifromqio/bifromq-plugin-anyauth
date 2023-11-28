#! /bin/bash

if [ $# -lt 4 ]; then
  echo "USAGE: $0 -c classname -f filename [-fg]"
  exit 1
fi

while [ $# -gt 0 ]; do
  COMMAND=$1
  case $COMMAND in
  -fg)
    FOREGROUND_MODE='true'
    shift 1
    ;;
  -c)
    NAME=$2
    shift 2
    ;;
  -f)
    FILE_NAME=$2
    shift 2
    ;;
  *)
    break
    ;;
  esac
done

BASE_DIR=$(
  cd "$(dirname "$0")"
  pwd
)/..

CONF_DIR="$BASE_DIR/conf"
CONF_FILE="$CONF_DIR/$FILE_NAME"
LOG_CONF_FILE="$CONF_DIR/logback.xml"
LIB_DIR="$BASE_DIR/lib"
CLASSPATH=$(echo "$LIB_DIR/*")

if [ "x$LOG_DIR" = "x" ]; then
  LOG_DIR="$BASE_DIR/logs"
fi
mkdir -p "$LOG_DIR"

pid() {
  echo "$(ps -ef | grep $NAME | grep java | grep -v grep | awk '{print $2}')"
}

total_memory() {
  if [ -n "$MEM_LIMIT" ]; then
    echo $MEM_LIMIT
  elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    local total_kb=$(cat /proc/meminfo | grep 'MemTotal' | awk -F : '{print $2}' | awk '{print $1}' | sed 's/^[ \t]*//g')
    echo "$((total_kb * 1024))"
  elif [[ "$OSTYPE" == "darwin"* ]]; then
    echo "$(sysctl hw.memsize | awk -F : '{print $2}' | xargs)"
  else
    echo -1
  fi
}

memory_in_mb() {
  echo $(($1 / 1024 / 1024))
}

if [ -n "$(pid)" ]; then
  echo "$NAME already started: $(pid)"
  exit 1
fi

MEMORY=$(total_memory)
echo -e "Total Memory: $(($MEMORY / 1024 / 1024 / 1024)) GB\n"

# get java version
if [ -z "$JAVA_HOME" ]; then
  JAVA="java"
else
  JAVA="$JAVA_HOME/bin/java"
fi

# GC options
if [ -z "$JVM_GC_OPTS" ]; then
  JVM_GC_OPTS="-XX:+UnlockExperimentalVMOptions \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:+UseZGC \
  -XX:ZAllocationSpikeTolerance=5 \
  -Xlog:async \
  -Xlog:gc:file='${LOG_DIR}/gc-%t.log:time,tid,tags:filecount=5,filesize=50m' \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath='${LOG_DIR}' \
"
fi

eval JVM_GC=("$JVM_GC_OPTS")
# Memory options
if [ -z "$JVM_HEAP_OPTS" ]; then
  MEMORY_FRACTION=70 # Percentage of total memory to use
  HEAP_MEMORY=$(($MEMORY /100 * $MEMORY_FRACTION))
  MIN_HEAP_MEMORY=$(($HEAP_MEMORY / 2))

  # Calculate max direct memory based on total memory
  MAX_DIRECT_MEMORY_FRACTION=20 # Percentage of total memory to use for max direct memory
  MAX_DIRECT_MEMORY=$(($MEMORY /100 * $MAX_DIRECT_MEMORY_FRACTION))

  META_SPACE_MEMORY=128m
  MAX_META_SPACE_MEMORY=500m

  # Set heap options
  JVM_HEAP_OPTS="-Xms$(memory_in_mb ${MIN_HEAP_MEMORY})m -Xmx$(memory_in_mb ${HEAP_MEMORY})m -XX:MetaspaceSize=${META_SPACE_MEMORY} -XX:MaxMetaspaceSize=${MAX_META_SPACE_MEMORY} -XX:MaxDirectMemorySize=${MAX_DIRECT_MEMORY}"
fi

if [ "x$FOREGROUND_MODE" = "xtrue" ]; then
  exec "$JAVA" $JVM_HEAP_OPTS $JVM_PERF_OPTS "${JVM_GC[@]}" $EXTRA_JVM_OPTS \
    -cp "$CLASSPATH" \
    -DLOG_DIR="$LOG_DIR" \
    -DCONF_DIR="$CONF_DIR" \
    -Dlogback.configurationFile="$LOG_CONF_FILE" \
    $NAME -c "$CONF_FILE"
else
  nohup "$JAVA" $JVM_HEAP_OPTS $JVM_PERF_OPTS "${JVM_GC[@]}" $EXTRA_JVM_OPTS \
    -cp "$CLASSPATH" \
    -DLOG_DIR="$LOG_DIR" \
    -DCONF_DIR="$CONF_DIR" \
    -Dlogback.configurationFile="$LOG_CONF_FILE" \
    $NAME -c "$CONF_FILE" >"${LOG_DIR}/stdout.log" 2>&1 </dev/null &
  PIDS=$(pid)
  echo "$NAME process started: $PIDS"
fi