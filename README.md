# A JWM Token Handler for Financial Grade SPAs

[![Quality](https://img.shields.io/badge/quality-experiment-red)](https://curity.io/resources/code-examples/status/)
[![Availability](https://img.shields.io/badge/availability-source-blue)](https://curity.io/resources/code-examples/status/)

## Overview

The token handler implements state-of-the-art Website security for Single Page Applications:

- Strongest browser security with only `SameSite=strict` cookies
- Financial-grade OpenID Connect flows and strong secrets

![Logical Components](/doc/logical-components.png)

## Architecture

The following endpoints are used so that the SPA uses simple one liners to perform its OAuth work:

| Endpoint | Description |
| -------- | ----------- |
| POST /login/start | Start a login by providing the request URL to the SPA and setting temporary cookies |
| POST /login/end | Complete a login and issuing secure cookies for the SPA containing encrypted tokens |
| GET /userInfo | Return Personally Identifiable Information (PII) fgor the SPA to display |
| POST /refresh | Ask the token handler to refresh an access token and rewrite cookies |
| POST /logout | Ask the token handler to clear cookies and return an end session request URL |

For further details see the [Architecture](/doc/Architecture.md) article.

## Test Driven Token Handler Development

See the [Setup.md](/doc/Setup.md) article for details on productive token handler development.\
This enables a test driven approach to developing the token handler, without the need for a browser.

## End-to-End SPA Flow

See the below tutorials for further details and to run an end-to-end solution in the browser:

- [Financial Grade SPA Code Example](https://curity.io/resources/learn/financial-grade-spa-example/)
- [Financial Grade Token Handler Code Example](https://curity.io/resources/learn/financial-grade-token-handler-example/)

## More Information

Please visit [curity.io](https://curity.io/) for more information about the Curity Identity Server.