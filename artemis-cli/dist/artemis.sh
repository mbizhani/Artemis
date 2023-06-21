#!/bin/bash

BASE_DIR="$(dirname "$(realpath "$0")")"

if [ "$1" ]; then
  java -Dfile.encoding=UTF-8 -jar "${BASE_DIR}"/artemis-cli.jar "$@"
else
  cd "${BASE_DIR}" || exit
  echo "Base Directory to Execute: ${BASE_DIR}"
  java -Dfile.encoding=UTF-8 -jar artemis-cli.jar exec
fi
