#!/bin/bash

VER='2.0'

rm -f dist/artemis-cli*.jar

mvn dependency:copy \
  -Dartifact=org.devocative.artemis:artemis-cli:${VER} \
  -DoutputDirectory=dist

mv dist/artemis-cli-${VER}.jar dist/artemis-cli.jar

(cd dist && zip -r - .) > artemis-${VER}.zip