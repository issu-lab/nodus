#!/bin/sh
APP_HOME=$(dirname "$0")
APP_HOME=$(cd "$APP_HOME" && pwd)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
if [ -z "$JAVA_HOME" ]; then
    JAVACMD=java
else
    JAVACMD=$JAVA_HOME/bin/java
fi
exec "$JAVACMD" -Xmx64m -Xms64m -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
