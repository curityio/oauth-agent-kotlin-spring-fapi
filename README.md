# A Java based Token Handler for Financial Grade SPAs

[![Quality](https://img.shields.io/badge/quality-experiment-red)](https://curity.io/resources/code-examples/status/)
[![Availability](https://img.shields.io/badge/availability-source-blue)](https://curity.io/resources/code-examples/status/)

## Overview

The token handler implements an API driven `Back End for Front End (BFF)` for Single Page Applications.\
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
| GET /userInfo | Return Personally Identifiable Information (PII) for the SPA to display |
| POST /refresh | Ask the token handler to refresh an access token and rewrite cookies |
| POST /logout | Ask the token handler to clear cookies and return an end session request URL |

For further details see the [Architecture](/doc/Architecture.md) article.

## Token Handler Development

Use `./gradlew test` command to run a suite of integration tests, that can help you during development.

See the [Setup](/doc/Setup.md) article for details on setting up a token handler development environment with an \
instance of the Curity Identity Server. This enables a test driven approach to developing the token handler, without \
the need for a browser.

## End-to-End SPA Flow

See the below article for details on how to run the end-to-end solution in a browser:

- [Financial Grade SPA Code Example](https://curity.io/resources/learn/token-handler-spa-example/)

## Website Documentation

See the [Curity OAuth for Web Home Page](https://curity.io/product/token-service/oauth-for-web/) for all resources on this design pattern.

## More Information

Please visit [curity.io](https://curity.io/) for more information about the Curity Identity Server.
