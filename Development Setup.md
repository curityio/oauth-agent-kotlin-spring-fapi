# Local Development Setup

This page shows how to run the API in a test driven manner, when developing the token handler 

## Prerequisites

- Ensure that Java 11 or above is installed
- Ensure that OpenSSL is installed
- Ensre that the jq tool is installed

## Update the Hosts File

Ensure that the hosts file contains the following development domain names:

```text
127.0.0.1  api.example.local login.example.local
:1 localhost
```

## Understand URLs

| Component | Base URL | Usage |
| --------- | -------- | ----- |
| Token Handler API | https://api.example.local:8080/tokenhandler | This acts as a Back End for Front End for SPAs |
| Curity Identity Server | https://login.example.local:8443 | This will receive a Mutual TLS secret from the token handler | 

## Generate Certificates

Run this script to create development certificates for the above domains: 

```bash
cd certs
./create-certs.sh
```

## Configure Java SSL Trust

Run a command of the following form from the root folder

```bash
sudo keytool -delete -alias example.local -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit -noprompt
sudo keytool -import -alias example.local -keystore "$JAVA_HOME/lib/security/cacerts" -file ./certs/example.ca.pem -storepass changeit -noprompt
```

## Build and Run the Token Handler API

Run this from the root folder and the API will listen on SSL over port 8080: 

```bash
./gradlew bootRun
```

Test that the API is contactable using this command from the root folder.\
This will result in an unauthorized request initially

```bash
curl --cacert ../certs/example.ca.pem -i -X POST https://api.example.local:8080/tokenhandler/login/start \
-H "origin: https://www.example.local" \
-d {}
```

## Test the Token Handler API

This script will run curl tests to verify the token handler workflow.\
The script will initially fail when it tries to call the Curity Identity Server:

```bash
cd test
./token-handler.sh
```