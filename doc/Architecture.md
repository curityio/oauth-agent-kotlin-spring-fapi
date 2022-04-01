# Architecture of the OAuth Agent

## Overview

A kotlin implementation of the OAuth Agent using the Spring framework. The API can be deployed to a host of your choice. This implementation uses financial-grade features for increased security of OAuth flows:

- Authorization requests use PAR.
- Authorization requests obtain responses in the form of a JWT, using the JARM spec.
- mTLS is uses as the client authentication method.

This OAuth Agent is a stateless one. It takes the token response from an Authorization Server, encrypts tokens and sets them in http-only cookies. Those cookies can then be used to get user info or call business APIs.

## Endpoints

The OAuth Agent exposes the following endpoints:

1. POST `/login/start`
2. POST `/login/end`
3. GET `/userInfo`
4. GET `/claims`
5. POST `/refresh`
6. POST `/logout`
 
### POST `/login/start`

This endpoint is used to initialize an authorization request. The API responds with a URL which the SPA should navigate to in order to start the authorization flow at the Authorization Server. The URL returned is a PAR URL. However, the format of the URL is irrelevant to the SPA, it should just redirect the user to that URL.

The OAuth Agent responds with a JSON containing the `authorizationRequestUrl` field.

#### Example request

`POST https://api.example.com/oauth-agent/login/start`

Response:
```json
{
  "authorizationRequestUrl": "https://idsvr.example.com/oauth/authorize?client_id=spa-client&request_uri=urn:ietf:params:oauth:request_uri:c0...43"
}
```

### POST `/login/end`

This endpoint should be be called by the SPA on any page load. The SPA sends the current URL to the OAuth Agent, which can either finish the authorization flow (if it was a response from the Authorization Server), or inform the SPA whether the user is logged in or not (basing on the presence of secure cookies).

#### Example request

```http
POST https://api.example.com/oauth-agent/login/end
pageUrl=https://www.example.com?response=eyJ...
```

The response will contain a few `Set-Cookie` headers.

### GET `/userInfo`

Endpoint which sends the access token to the user info endpoint, then returns data.

#### Example

```http
GET https://api.example.com/oauth-agent
Cookie: example-at=2558e7806c0523fd96d105...
```

Response

```json
{
  "sub": "0abd0b16b309a3a034af8494aa0092aa42813e635f194c795df5006db90743e8",
  "preferred_username": "demouser",
  "given_name": "Demo",
  "updated_at": 1627313147,
  "family_name": "User"
}
```

### GET `/claims`

Endpoint which returns claims of the ID token contained in the session cookie.

#### Example

```http
GET https://api.example.com/oauth-agent/claims
Cookie: example-id=2558e7806c0523fd96d105...
```

Response

```json
{
  "exp":1626263589,
  "nbf":1626259989,
  "jti":"34e76304-0bc3-46ee-bc70-e21685eb5282",
  "iss":"https://idsvr.example.com/oauth",
  "aud":"spa-client",
  "sub":"0abd0b16b309a3a034af8494aa0092aa42813e635f194c795df5006db90743e8",
  "auth_time":1626259937,
  "iat":1626259989
}
```

### POST `/refresh`

This endpoint can be called to force the OAuth Agent to refresh the access token. If the OAuth Agent is able to perform the refresh new cookies will be set in the response (which is a 204 response), otherwise the OAuth Agent will respond with a 401 response (e.g. when the refresh token is expired) to inform the SPA that a new login is required. 

### POST `/logout`

This endpoint can be called to get a logout URL. The SPA should navigate the user to that URL in order to perform a logout in the Authorization Server. The OAuth Agent also sets empty session cookies in the response.
