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
TRUSTSTORE_DESCRIPTION='Self Signed CA for example.local'

#
# SSL certificate parameters
#
KEYSTORE_FILE_PREFIX='example.local'
KEYSTORE_PASSWORD='Password1'
WILDCARD_DOMAIN_NAME='*.example.local'

#
# Create the root certificate public + private key protected by a passphrase
#
openssl genrsa -out $TRUSTSTORE_FILE_PREFIX.key 2048
echo '*** Successfully created Root CA key'

#
# Create the public key root certificate file, which has a long lifetime
#
openssl req \
    -x509 \
    -new \
    -nodes \
    -key $TRUSTSTORE_FILE_PREFIX.key \
    -out $TRUSTSTORE_FILE_PREFIX.pem \
    -subj "/CN=$TRUSTSTORE_DESCRIPTION" \
    -reqexts v3_req \
    -extensions v3_ca \
    -sha256 \
    -days 3650
echo '*** Successfully created Root CA'

#
# Create the SSL key
#
openssl genrsa -out $KEYSTORE_FILE_PREFIX.key 2048
echo '*** Successfully created SSL key'

#
# Create the certificate signing request file
#
openssl req \
    -new \
    -key $KEYSTORE_FILE_PREFIX.key \
    -out $KEYSTORE_FILE_PREFIX.csr \
    -subj "/CN=$WILDCARD_DOMAIN_NAME"
echo '*** Successfully created SSL certificate signing request'

#
# Create the SSL certificate and private key, which must have a limited lifetime
#
openssl x509 -req \
    -in $KEYSTORE_FILE_PREFIX.csr \
    -CA $TRUSTSTORE_FILE_PREFIX.pem \
    -CAkey $TRUSTSTORE_FILE_PREFIX.key \
    -CAcreateserial \
    -out $KEYSTORE_FILE_PREFIX.pem \
    -sha256 \
    -days 365 \
    -extfile server.ext
echo '*** Successfully created SSL certificate'

#
# Export the SSL certificate to a deployable PKCS#12 file that is password protected
#
openssl pkcs12 \
    -export -inkey $KEYSTORE_FILE_PREFIX.key \
    -in $KEYSTORE_FILE_PREFIX.pem \
    -name $WILDCARD_DOMAIN_NAME \
    -out $KEYSTORE_FILE_PREFIX.p12 \
    -passout pass:$KEYSTORE_PASSWORD
echo '*** Successfully exported SSL certificate to a PKCS#12 file'

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
rm example.srl
rm example.local.csr
