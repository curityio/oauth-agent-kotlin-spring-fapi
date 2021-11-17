# Local Development Setup

This page shows how to run the API in a test driven manner, when developing the token handler 

## Prerequisites

- Ensure that Java 11 or above is installed
- Ensure that OpenSSL is installed
- Ensure that Docker Desktop is installed
- Ensre that the jq tool is installed
- Ensure that you have a license file for the Curity Identity Server

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

Run the following command from the root folder to configure the token handler API to trust the root certificate:  

```bash
sudo keytool -import -alias example.local -keystore "$JAVA_HOME/lib/security/cacerts" -file ./certs/example.ca.pem -storepass changeit -noprompt
```

Remove trust when finished with testing or if you need to update the root certificate: 

```bash
sudo keytool -delete -alias example.local -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit -noprompt
```

## Build and Run the Token Handler API

Run this command from the root folder and the API will then listen on SSL over port 8080: 

```bash
./gradlew bootRun
```

Test that the API is contactable using this command from the root folder.\
This will result in an unauthorized request initially:

```bash
curl --cacert ./certs/example.ca.pem -i -X POST https://api.example.local:8080/tokenhandler/refresh \
-H "origin: https://www.example.local" \
-d {}
```

## Test the Token Handler API

This script will run curl tests to verify the token handler workflow.\
The script will initially fail when it tries to call the Curity Identity Server:

```bash
cd test
./test-token-handler.sh
```

## Deploy the Curity Identity Server

Copy a license.json file into the test/idsvr folder and then run these commands.\
This will make the 

```bash
cd test/idsvr
./deploy.sh
```

## Complete Testing

Run the complete test workflow again to ensure that all operations are working:

```bash
cd test
./test-token-handler.sh
```