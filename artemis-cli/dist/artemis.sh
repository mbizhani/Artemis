#!/bin/bash

BASE_DIR="$(dirname "$(realpath "$0")")"
echo "Base Directory to Execute: ${BASE_DIR}"
cd "${BASE_DIR}" || exit

if [ "$1" ]; then
  java -Dfile.encoding=UTF-8 -jar artemis-cli.jar "$@"
else
  java -Dfile.encoding=UTF-8 -jar artemis-cli.jar exec
fi
