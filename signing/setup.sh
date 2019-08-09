#!/bin/bash

ENCRYPT_KEY="$1"

if [ ! -z "$ENCRYPT_KEY" ]; then
    # Decrypt release key
    openssl aes-256-cbc -md sha256 -d -in signing/app-release.aes -out signing/app-release.jks -k "$ENCRYPT_KEY"

    # Decrypt signing properties
    openssl aes-256-cbc -md sha256 -d -in signing/keystore.properties.aes -out signing/keystore.properties -k "$ENCRYPT_KEY"
else
    echo "ENCRYPT_KEY is empty"
fi
