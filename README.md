# A JWM Token Handler for Financial Grade SPAs

[![Quality](https://img.shields.io/badge/quality-experiment-red)](https://curity.io/resources/code-examples/status/)
[![Availability](https://img.shields.io/badge/availability-source-blue)](https://curity.io/resources/code-examples/status/)

A kotlin implementation of the Token Handler API using the Spring framework. The API can be deployed to a host of your choice. This implementation uses financial-grade features for increased security of OAuth flows:

- Authorization requests use PAR.
- Authorization requests obtain responses in the form of a JWT, using the JARM spec.
- mTLS is uses as the client authentication method.

This BFF is a stateless one. It takes the token response from an Authorization Server, encrypts tokens and sets them in http-only cookies. Those cookies can then be used to get user info or call business APIs.

## Endpoints of the Token Handler API

The Token Handler API exposes the following endpoints:

1. POST `/login/start`
2. POST `/login/end`
3. GET `/userInfo`
4. POST `/logout`
5. POST `/refresh`

### POST `/login/start`

This endpoint is used to initialize an authorization request. The API responds with a URL which the SPA should navigate to in order to start the authorization flow at the Authorization Server. The URL returned is a PAR URL. However, the format of the URL is irrelevant to the SPA, it should just redirect the user to that URL.

The Token Handler responds with a JSON containing the `authorizationRequestUrl` field.

#### Example request

`POST https://api.example.com/tokenhandler/login/start`

Response:
```json
{
  "authorizationRequestUrl": "https://idsvr.example.com/oauth/authorize?client_id=spa_client&request_uri=urn:ietf:params:oauth:request_uri:c0...43"
}
```

### POST `/login/end`

This endpoint should be be called by the SPA on any page load. The SPA sends the current URL to the TH, which can either finish the authorization flow (if it was a response from the Authorization Server), or inform the SPA whether the user is logged in or not (basing on the presence of TH cookies).

#### Example request

```http
POST https://api.example.com/tokenhandler/login/end
pageUrl=https://www.example.com?response=eyJ...
```

The response will contain a few `Set-Cookie` headers.

### GET `/userInfo`

Endpoint which returns claims of the ID token contained in the session cookie.

#### Example

```http
GET https://api.example.com/tokenhandler
Cookie: myBFFSess=2558e7806c0523fd96d105...
```

Response

```json
{
  "exp":1626263589,
  "nbf":1626259989,
  "jti":"34e76304-0bc3-46ee-bc70-e21685eb5282",
  "iss":"https://login.example.com/oauth",
  "aud":"th-client",
  "sub":"user",
  "auth_time":1626259937,
  "iat":1626259989
}
```

### POST `/logout`

This endpoint can be called to get a logout URL. The SPA should navigate the user to that URL in order to perform a logout in the Authorization Server. The TH also sets empty session cookies in the response.

### POST `/refresh`

This endpoint can be called to force the TH to refresh the access token. If the TH is able to perform the refresh new cookies will be set in the response (which is a 204 response), otherwise the TH will respond with a 401 response (e.g. when the refresh token is expired) to inform the SPA that a new login is required. 

## Running the Token Handler Locally

Follow the below steps to get set up for developing and testing the token handler:

### Prerequisites

- Ensure that Java 11 or above is installed
- Ensure that OpenSSL is installed
- Ensure that Docker Desktop is installed
- Ensre that the jq tool is installed
- Get a license file for the Curity Identity Server with financial grade features

### Update the Hosts File

Ensure that the hosts file contains the following development domain names:

```text
127.0.0.1  api.example.local login.example.local
:1 localhost
```

### Understand URLs

| Component | Base URL | Usage |
| --------- | -------- | ----- |
| Token Handler API | https://api.example.local:8080/tokenhandler | This acts as a Back End for Front End for SPAs |
| Curity Identity Server | https://login.example.local:8443 | This will receive a Mutual TLS secret from the token handler | 

### Generate Certificates

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

### Build and Run the Token Handler API

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

### Deploy the Curity Identity Server

Copy a license.json file with support for financial grade features into the test/idsvr folder and then run these commands:

```bash
cd test/idsvr
./deploy.sh
```

### Test the Token Handler API

The test script can be used to test the token handler's operations using curl scripts:

```bash
cd test
./test-token-handler.sh
```
