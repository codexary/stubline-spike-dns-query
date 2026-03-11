#!/bin/sh
set -eu
MAVEN_PROJECTBASEDIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MAVEN_USER_HOME=${MAVEN_USER_HOME:-"$MAVEN_PROJECTBASEDIR/.mvn"}
export MAVEN_USER_HOME
exec java -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" -cp "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain "$@"
