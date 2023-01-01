# A Java based OAuth Agent for Financial Grade SPAs

[![Quality](https://img.shields.io/badge/quality-test-yellow)](https://curity.io/resources/code-examples/status/)
[![Availability](https://img.shields.io/badge/availability-source-blue)](https://curity.io/resources/code-examples/status/)

## Overview

The OAuth Agent provides a modern API driven `Back End for Front End (BFF)` for Single Page Applications.\
This implementation provides state-of-the-art security suitable for financial-grade:

- Strongest browser security with only `SameSite=strict` cookies
- Financial-grade OpenID Connect flow using [PAR](https://tools.ietf.org/id/draft-lodderstedt-oauth-par-00.html),  [JARM](https://openid.net/specs/openid-financial-api-jarm.html) and a Mutual TLS secret

![Logical Components](/doc/logical-components.png)

## Architecture

The following endpoints are used so that the SPA uses simple one liners to perform its OAuth work:

| Endpoint | Description |
| -------- | ----------- |
| POST /login/start | Start a login by providing the request URL to the SPA and setting temporary cookies |
| POST /login/end | Complete a login and issuing secure cookies for the SPA containing encrypted tokens |
| GET /userInfo | Return information from the User Info endpoint for the SPA to display |
| GET /claims | Return ID token claims such as `auth_time` and `acr` |
| POST /refresh | Refresh an access token and rewrite cookies |
| POST /logout | Clear cookies and return an end session request URL |

For further details see the [Architecture](/doc/Architecture.md) article.

## Deployment

Build the OAuth agent into a Docker image:

```bash
./gradlew bootJar
docker build -t oauthagent:1.0.0 .
```

Then deploy the Docker image with environment variables similar to these:

```yaml
oauth-agent:
  image: oauthagent:1.0.0
  hostname: oauthagent-host
  environment:
    PORT: 3001
    TRUSTED_WEB_ORIGIN: 'https://www.example.com'
    AUTHORIZE_ENDPOINT: 'https://login-internal/oauth/v2/oauth-authorize'
    AUTHORIZE_EXTERNAL_ENDPOINT: 'https://login.example.com/oauth/v2/oauth-authorize'
    TOKEN_ENDPOINT: 'https://login-internal/oauth/v2/oauth-token'
    JWKS_URI: 'https://login-internal/oauth/v2/oauth-anonymous/jwks'
    USERINFO_ENDPOINT: 'https://login-internal/oauth/v2/oauth-userinfo'
    LOGOUT_ENDPOINT: 'https://login.example.com/oauth/v2/oauth-session/logout'
    CLIENT_ID: 'spa-client'
    CLIENT_SECRET: 'Password1'
    REDIRECT_URI: 'https://www.example.com/'
    POST_LOGOUT_REDIRECT_URI: 'https:www.example.com/'
    SCOPE: 'openid profile'
    COOKIE_DOMAIN: 'api.example.com'
    COOKIE_NAME_PREFIX: 'example'
    COOKIE_ENCRYPTION_KEY: 'fda91643fce9af565bdc34cd965b48da75d1f5bd8846bf0910dd6d7b10f06dfe'
    CORS_ENABLED: 'true'
    SERVER_CERT_P12_PATH: '/certs/myserver.p12'
    SERVER_CERT_P12_PASSWORD: 'Password1'
    CLIENT_CERT_P12_PATH: './certs/myclient.p12'
    CLIENT_CERT_P12_PASSWORD: 'Password1'
    CA_CERT_PEM_PATH: './certs/myrootca.pem'
```

If the OAuth Agent is deployed to the web domain, then set these properties:

```yaml
COOKIE_DOMAIN: 'www.example.com'
CORS_ENABLED: 'false'
```

## OAuth Agent Development

See the [Setup](/doc/Setup.md) article for details on setting up an OAuth Agent development environment with an \
instance of the Curity Identity Server. This enables a test driven approach to developing the OAuth Agent, without \
the need for a browser.

## End-to-End SPA Flow

See the below article for details on how to run the end-to-end solution in a browser:

- [Financial Grade SPA Code Example](https://curity.io/resources/learn/token-handler-spa-example/)

## Website Documentation

See the [Curity OAuth for Web Home Page](https://curity.io/product/token-service/oauth-for-web/) for all resources on this design pattern.

## More Information

Please visit [curity.io](https://curity.io/) for more information about the Curity Identity Server.
