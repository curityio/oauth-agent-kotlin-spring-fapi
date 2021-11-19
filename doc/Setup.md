# How to Develop the Token Handler

Follow the below steps to get set up for developing and testing the financial-grade token handler.

## Prerequisites

- Ensure that Java 11 or above is installed
- Ensure that OpenSSL is installed
- Ensure that Docker Desktop is installed
- Ensure that the jq tool is installed

Also get a License File for the Curity Identity Server with support for financial-grade features:

- Sign in to the [Curity Developer Portal](https://developer.curity.io/) with your Github account.
- You can get a [Free Community Edition License](https://curity.io/product/community/) if you are new to the Curity Identity Server.

## Update your Hosts File

Ensure that the hosts file contains the following development domain names:

```text
127.0.0.1  api.example.local login.example.local
:1 localhost
```

## Understand URLs

For local development of the token handler the following URLs are used:

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
sudo keytool -import -alias example.ca -keystore "$JAVA_HOME/lib/security/cacerts" -file ./certs/example.ca.pem -storepass changeit -noprompt
```

Remove trust when finished with testing or if you need to update the root certificate: 

```bash
sudo keytool -delete -alias example.ca -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit -noprompt
```

## Build and Run the Token Handler API

Run this command from the root folder and the API will then listen on SSL over port 8080.\
Alternatively the API can be run in an IDE of your choice:

```bash
./gradlew bootRun
```

Test that the API is contactable by running this command from the root folder:

```bash
curl --cacert ./certs/example.ca.pem -i -X POST https://api.example.local:8080/tokenhandler/refresh \
-H "origin: https://www.example.local" \
-d {}
```

## Deploy the Curity Identity Server

Copy a license file into the `test/idsvr` folder and then run the following commands:

```bash
cd test/idsvr
./deploy.sh
```

## Test the Token Handler API

The test script can then be used to verify the token handler's operations using the curl tool:

```bash
cd test
./test-token-handler.sh
```

![API Tests](api-tests.png)

## Free Docker Resources

When finished with your development session, free Docker resources like this:

```bash
cd test/idsvr
./teardown.sh
```