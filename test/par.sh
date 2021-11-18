#!/bin/bash

#export https_proxy='http://127.0.0.1:8888'

curl --cacert ../certs/example.ca.pem \
--cert '../certs/example.client.p12':'Password1' \
--cert-type P12 \
-X POST 'https://login.example.local:8443/oauth/v2/oauth-authorize/par' \
-H 'Content-Type: application/x-www-form-urlencoded' \
-d 'client_id=spa-client' \
-d 'state=MkwrS4ZkPLzmSenAYbRLSgfAH6lfM2JfPFTlTZtQngWOAZLKdd0o6RjLFdnhQ3OI' \
-d 'response_mode=jwt' \
-d 'response_type=code' \
-d 'redirect_uri=https://www.example.local/' \
-d 'code_challenge=dh0xsKEjEqQ-97Y39A20qgc2DkbPGvKqy2BzwVwtL6M' \
-d 'code_challenge_method=S256' \
-d 'scope=openid profile'
