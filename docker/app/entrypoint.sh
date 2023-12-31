#!/bin/sh

if [[ -z $APP_HOME ]]; then
    $APP_HOME="/app"
fi

JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom -DlogPath=$APP_HOME/logs $JAVA_OPTS"
START_CMD="java $JAVA_OPTS -jar $APP_HOME/iassign.jar"
echo "java command is: $START_CMD"

java $JAVA_OPTS -jar $APP_HOME/iassign.jar