#!/bin/bash

openssl req -x509 -sha256 -days 365 -newkey rsa:2048 -nodes -keyout my-key.pem -out my-cert.pem

openssl pkcs12 -export -out my.pfx -inkey my-key.pem -in my-cert.pem -passin pass:"mypass" -passout pass:"mypass" -name "myalias"