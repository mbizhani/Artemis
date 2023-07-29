@ECHO off

CHCP 1252 > NUL

SET BASE_DIR="%~dp0"
ECHO Base Directory to Execute: %BASE_DIR%


PUSHD %BASE_DIR%

IF "%1"=="" (
  java -Dfile.encoding=UTF-8 -jar artemis-cli.jar exec
) ELSE (
  java -Dfile.encoding=UTF-8 -jar artemis-cli.jar %*%
)

POPD