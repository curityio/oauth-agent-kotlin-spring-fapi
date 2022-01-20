#!/bin/bash

########################################################################
# A script to create development certificates used for local development
########################################################################

#
# Fail on first error
#
set -e

#
# Point to the OpenSsl configuration file for the platform
#
case "$(uname -s)" in

  # Mac OS
  Darwin)
    export OPENSSL_CONF='/System/Library/OpenSSL/openssl.cnf'
 	;;

  # Windows with Git Bash
  MINGW64*)
    export OPENSSL_CONF='C:/Program Files/Git/usr/ssl/openssl.cnf';
    export MSYS_NO_PATHCONV=1;
	;;
esac

#
# Root certificate parameters
#
TRUSTSTORE_FILE_PREFIX='example.ca'
TRUSTSTORE_PASSWORD='Password1'
TRUSTSTORE_NAME='Self Signed CA for example.local'

#
# Server certificate parameters
#
SERVER_KEYSTORE_FILE_PREFIX='example.server'
SERVER_KEYSTORE_PASSWORD='Password1'
WILDCARD_DOMAIN_NAME='*.example.local'

#
# Client certificate parameters used for Mutual TLS
#
CLIENT_KEYSTORE_FILE_PREFIX='example.client'
CLIENT_KEYSTORE_NAME='financial-grade-spa, OU=Example, O=Curity AB, C=SE'
CLIENT_KEYSTORE_PASSWORD='Password1'

#
# Create the root certificate public + private key protected by a passphrase
#
openssl genrsa -out $TRUSTSTORE_FILE_PREFIX.key 2048
echo '*** Successfully created Root CA key'

openssl req \
    -x509 \
    -new \
    -nodes \
    -key $TRUSTSTORE_FILE_PREFIX.key \
    -out $TRUSTSTORE_FILE_PREFIX.pem \
    -subj "/CN=$TRUSTSTORE_NAME" \
    -reqexts v3_req \
    -extensions v3_ca \
    -sha256 \
    -days 3650
echo '*** Successfully created Root CA'

#
# Create the SSL wildcard certificate and key, exported to a password protected P12 file
#
openssl genrsa -out $SERVER_KEYSTORE_FILE_PREFIX.key 2048
echo '*** Successfully created server key'

openssl req \
    -new \
    -key $SERVER_KEYSTORE_FILE_PREFIX.key \
    -out $SERVER_KEYSTORE_FILE_PREFIX.csr \
    -subj "/CN=$WILDCARD_DOMAIN_NAME"
echo '*** Successfully created server certificate signing request'

openssl x509 -req \
    -in $SERVER_KEYSTORE_FILE_PREFIX.csr \
    -CA $TRUSTSTORE_FILE_PREFIX.pem \
    -CAkey $TRUSTSTORE_FILE_PREFIX.key \
    -CAcreateserial \
    -out $SERVER_KEYSTORE_FILE_PREFIX.pem \
    -sha256 \
    -days 365 \
    -extfile extensions.cnf \
    -extensions server_ext
echo '*** Successfully created server certificate'

openssl pkcs12 \
    -export -inkey $SERVER_KEYSTORE_FILE_PREFIX.key \
    -in $SERVER_KEYSTORE_FILE_PREFIX.pem \
    -name $WILDCARD_DOMAIN_NAME \
    -out $SERVER_KEYSTORE_FILE_PREFIX.p12 \
    -passout pass:$SERVER_KEYSTORE_PASSWORD
echo '*** Successfully exported server certificate to a PKCS#12 file'

#
# Create the client certificate that the example merchant will use
#
openssl genrsa -out $CLIENT_KEYSTORE_FILE_PREFIX.key 2048
echo '*** Successfully created client key'

openssl req \
    -new \
    -key $CLIENT_KEYSTORE_FILE_PREFIX.key \
    -out $CLIENT_KEYSTORE_FILE_PREFIX.csr \
    -subj "/CN=$CLIENT_KEYSTORE_NAME"
echo '*** Successfully created client certificate signing request'

openssl x509 -req \
    -in $CLIENT_KEYSTORE_FILE_PREFIX.csr \
    -CA $TRUSTSTORE_FILE_PREFIX.pem \
    -CAkey $TRUSTSTORE_FILE_PREFIX.key \
    -CAcreateserial \
    -out $CLIENT_KEYSTORE_FILE_PREFIX.pem \
    -sha256 \
    -days 365 \
    -extfile extensions.cnf \
    -extensions client_ext
echo '*** Successfully created client certificate'

openssl pkcs12 \
    -export -inkey $CLIENT_KEYSTORE_FILE_PREFIX.key \
    -in $CLIENT_KEYSTORE_FILE_PREFIX.pem \
    -name $CLIENT_KEYSTORE_FILE_PREFIX \
    -out $CLIENT_KEYSTORE_FILE_PREFIX.p12 \
    -passout pass:$CLIENT_KEYSTORE_PASSWORD
echo '*** Successfully exported client certificate to a PKCS#12 file'

#
# Java trust stores work best when also password protected, so use a P12 file for the root also
#
openssl pkcs12 \
    -export -inkey $TRUSTSTORE_FILE_PREFIX.key \
    -in $TRUSTSTORE_FILE_PREFIX.pem \
    -name $TRUSTSTORE_FILE_PREFIX \
    -out $TRUSTSTORE_FILE_PREFIX.p12 \
    -passout pass:$TRUSTSTORE_PASSWORD
echo '*** Successfully exported root CA to a PKCS#12 file'

#
# Remove files we no longer need
#
rm example.server.csr
rm example.client.csr
rm example.ca.srl
