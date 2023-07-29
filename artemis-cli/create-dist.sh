#!/bin/bash

VER='2.0'
ARCH_NAME="artemis-v${VER}"


##
# DOWNLOAD CLI JAR FILE
rm -f dist/artemis-cli*.jar

mvn dependency:copy \
  -Dartifact=org.devocative.artemis:artemis-cli:${VER} \
  -DoutputDirectory=dist

mv dist/artemis-cli-${VER}.jar dist/artemis-cli.jar


##
# CREATE ZIP & TAR.GZ ARCHIVE
cd dist/ || exit

zip -r ../${ARCH_NAME}.zip .
tar cvfz ../${ARCH_NAME}.tar.gz *