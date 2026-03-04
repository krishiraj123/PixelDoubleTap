#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Use the maximum available, or set MAX_FD != -1 to disable.
maxFds() {
  if [ -n "$MAX_FD" ]; then
    echo "$MAX_FD"
    return
  fi
  # default fallback
  echo 64000
}

CLASSPATH="$PWD/gradle/wrapper/gradle-wrapper.jar"

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS=''

# Find java executable
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/bin/java" ] ; then
        JAVACMD="$JAVA_HOME/bin/java"
    else
        JAVACMD="$JAVA_HOME/jre/bin/java"
    fi
fi
if [ -z "$JAVACMD" ] ; then
    JAVACMD="java"
fi

# Execute Gradle Wrapper main class
exec "$JAVACMD" $DEFAULT_JVM_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"