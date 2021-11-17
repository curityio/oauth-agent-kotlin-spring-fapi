# A Token Handler backend component

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

`POST https://th.example.com/login/start`

Response:
```json
{
  "authorizationRequestUrl": "https://idsvr.example.com/oauth/authorize?client_id=bff_client&request_uri=urn:ietf:params:oauth:request_uri:c0...43"
}
```

### POST `/login/end`

This endpoint should be be called by the SPA on any page load. The SPA sends the current URL to the TH, which can either finish the authorization flow (if it was a response from the Authorization Server), or inform the SPA whether the user is logged in or not (basing on the presence of TH cookies).

#### Example request

```http
POST https://th.example.com/login/end
pageUrl=https://www.example.com?response=eyJ...
```

The response will contain a few `Set-Cookie` headers.

### GET `/userInfo`

Endpoint which returns claims of the ID token contained in the session cookie.

#### Example

```http
GET https://th.example.com
Cookie: myBFFSess=2558e7806c0523fd96d105...
```

Response

```json
{
  "exp":1626263589,
  "nbf":1626259989,
  "jti":"34e76304-0bc3-46ee-bc70-e21685eb5282",
  "iss":"https://idsvr.example.com/oauth",
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

## Running the Token Handler

To run the Token Handler follow these steps:

- In `src/main/resources/application.yml` set proper configuration for your environment: client ID, Authorization Server endpoints, scopes required, etc.
- Also in `application.yml` in the SSL section set proper files and passwords for the keystore (the certificate used by the client to identify itself) and the trustore (the certificate of your Authorization Server). The certificate store files should be located in the `src/main/resources` folder.
- Make sure that the client is properly configured in your Authorization Server:
   - client authentication is mTLS with the same certificate as in the keystore
   - the client can use PAR
   - the client can use JARM responses
- Make sure that the Authorization Server's certificate is also added to your Java's cacert keystore. The jose4j library will call the JWKS endpoint of the Authorization Server, and it must be able to verify the AS's certificate. 

Now you can run the Token Handler. You can use the example SPA to test the Token Handler: https://github.com/curityio/web-oauth-via-bff/tree/main/code/spa.
