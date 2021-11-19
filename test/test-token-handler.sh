#!/bin/bash

##################################################################
# Tests to run against token handler endpoints outside the browser
##################################################################

BFF_API_BASE_URL='https://api.example.local:8080/tokenhandler'
WEB_BASE_URL='https://www.example.local'
AUTHORIZATION_SERVER_BASE_URL='https://login.example.local:8443'
RESPONSE_FILE=data/response.txt
MAIN_COOKIES_FILE=data/main_cookies.txt
LOGIN_COOKIES_FILE=data/login_cookies.txt
CURITY_COOKIES_FILE=data/curity_cookies.txt
#export https_proxy='http://127.0.0.1:8888'

#
# Ensure that we are in the folder containing this script
#
cd "$(dirname "${BASH_SOURCE[0]}")"

#
# Get a header value from the HTTP response file
#
function getHeaderValue(){
  local _HEADER_NAME=$1
  local _HEADER_VALUE=$(cat $RESPONSE_FILE | grep -i "^$_HEADER_NAME" | sed -r "s/^$_HEADER_NAME: (.*)$/\1/i")
  local _HEADER_VALUE=${_HEADER_VALUE%$'\r'}
  echo $_HEADER_VALUE
}

#
# Temp data is stored in this folder
#
mkdir -p data

#
# Test sending an invalid web origin to the BFF API in an OPTIONS request
# The logic around CORS is configured, not coded, so ensure that it works as expected
#
echo '1. Testing OPTIONS request with an invalid web origin ...'
HTTP_STATUS=$(curl -k -i -s -X OPTIONS "$BFF_API_BASE_URL/login/start" \
-H "origin: https://malicious-site.com" \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" == '000' ]; then
  echo '*** Connectivity problem encountered, please check endpoints and whether an HTTP proxy tool is running'
  exit
fi
ORIGIN=$(getHeaderValue 'Access-Control-Allow-Origin')
if [ "$ORIGIN" != '' ]; then
  echo '*** CORS access was granted to a malicious origin'
  exit
fi
echo '1. OPTIONS with invalid web origin was not granted access'

#
# Test sending a valid web origin to the BFF API in an OPTIONS request
#
echo '2. Testing OPTIONS request with a valid web origin ...'
HTTP_STATUS=$(curl -k -i -s -X OPTIONS "$BFF_API_BASE_URL/login/start" \
-H "origin: $WEB_BASE_URL" \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '200'  ] && [ "$HTTP_STATUS" != '204' ]; then
  echo "*** Problem encountered requesting cross origin access, status: $HTTP_STATUS"
  exit
fi
ORIGIN=$(getHeaderValue 'Access-Control-Allow-Origin')
if [ "$ORIGIN" != "$WEB_BASE_URL" ]; then
  echo '*** The Access-Control-Allow-Origin response header has an unexpected value'
  exit
fi
echo '2. OPTIONS with valid web origin granted access successfully'

#
# Next we will test an unauthenticated page load but first test CORS
# The logic around trusted origins is coded by us
#
echo '3. Testing end login POST with invalid web origin ...'
HTTP_STATUS=$(curl -k -i -s -X POST "$BFF_API_BASE_URL/login/end" \
-H "origin: https://malicious-site.com" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-d '{"pageUrl":"'$WEB_BASE_URL'"}' \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '403' ]; then
  echo '*** End login did not fail as expected'
  exit
fi
echo '3. POST to endLogin with an invalid web origin was successfully rejected'

#
# Test sending an end login request to the API as part of an unauthenticated page load
#
echo '4. Testing end login POST for an unauthenticated page load ...'
HTTP_STATUS=$(curl -k -i -s -X POST "$BFF_API_BASE_URL/login/end" \
-H "origin: $WEB_BASE_URL" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-d '{"pageUrl":"'$WEB_BASE_URL'"}' \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '200' ]; then \
  echo "*** Unauthenticated page load failed with status $HTTP_STATUS"
  exit
fi
JSON=$(tail -n 1 $RESPONSE_FILE) 
echo $JSON | jq
IS_LOGGED_IN=$(jq -r .isLoggedIn <<< "$JSON")
HANDLED=$(jq -r .handled <<< "$JSON")
if [ "$IS_LOGGED_IN" != 'false'  ] || [ "$HANDLED" != 'false' ]; then
   echo "*** End login returned an unexpected payload"
   exit
fi
echo '4. POST to endLogin for an unauthenticated page load completed successfully'

#
# Test sending a start login request to the API with an invalid origin header
# The logic around trusted origins is coded by us
#
echo '5. Testing POST to start login from invalid web origin ...'
HTTP_STATUS=$(curl -k -i -s -X POST "$BFF_API_BASE_URL/login/start" \
-H "origin: https://malicious-site.com" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '403' ]; then
  echo '*** Start Login with an invalid web origin did not fail as expected'
  exit
fi
echo '5. POST to startLogin with invalid web origin was not granted access'

#
# Test sending a valid start login request to the API
#
echo '6. Testing POST to start login ...'
HTTP_STATUS=$(curl -k -i -s -X POST "$BFF_API_BASE_URL/login/start" \
-H "origin: $WEB_BASE_URL" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-c $LOGIN_COOKIES_FILE \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '200' ]; then
  echo "*** Start login failed with status $HTTP_STATUS"
  exit
fi
JSON=$(tail -n 1 $RESPONSE_FILE)
echo $JSON | jq
echo "6. POST to start login succeeded and returned the authorization request URL"

#
# Next perform a login to get the URL returned to the web client
#
echo '7. Performing API driven login ...'
./login.sh
if [ "$?" != '0' ]; then
  echo '*** Problem encountered implementing an API driven login'
  exit
fi

#
# Next verify that the OAuth state is correctly verified against the request value
#
echo '8. Testing posting a previous response into the browser ...'
APP_URL="$WEB_BASE_URL?response=eyJraWQiOiItMTg2MTcwNTU3MCIsIng1dCI6Inl6VFBLMlZrRnlyb2pzbDhTNlNXa3BtR2VGVSIsImFsZyI6IlJTMjU2In0.eyJleHAiOjE2MzcyMjk2MjMsImlzcyI6Imh0dHBzOi8vbG9naW4uZXhhbXBsZS5sb2NhbDo4NDQzL29hdXRoL3YyL29hdXRoLWFub255bW91cyIsImF1ZCI6InNwYS1jbGllbnQiLCJpYXQiOjE2MzcyMjk2MDMsInB1cnBvc2UiOiJhdXRoel9yZXNwb25zZSIsImNvZGUiOiJZUGtnVEhDVTg0YkFnM0ZzZU1LQUdmVTdRZDk5TGlvQiIsInN0YXRlIjoiTVRKM3FCdWxDZ0sxc1hFcWpyaUlsUVdGM25jbnlRNTlXSkxEUlJ4YTl6emlpY2VRbFV4eGFtVldDTVpPRFhNbyIsInNlc3Npb25fc3RhdGUiOiJwUDJsVFo2UnR0cFpscENhMU5DWXV2VkNoeXZVSVpNeWVJMUp4bEY5RnlRXHUwMDNkLk5HcVF6TWJ4THFzZCJ9.RaRx9cvNRpzkagNdfePNoxW4aQfcG3KbG5__ysF-nkeFiffDX66hnYvTiU9FNolCu4uCTsN2QqowIyf3FYi4KfH5cKn6kAoQPvDbxUcYL2oNTpCiT16M5Q9bb8kJaFJfOFT0-bxDTYpbdzOm3TmTkyr74M5cnuunluBr3I34sRwrejz9dP9cLQnTfyVuMhtS_2niDcjIxkNu6bMKtNUc3_Ww2oidzSIxxCL0WAxsfDqwpGswDsssG45PnEIh3spb6kCiwR5wjJNTcBBxYyixjXQ_EoyZ-ASonK1P2fTR1LKK3Y79wvmjmQXYo3F4rujPt9wTa5ydu9Ic36D7pibaHA"
PAGE_URL_JSON='{"pageUrl":"'$APP_URL'"}'
HTTP_STATUS=$(curl -k -i -s -X POST "$BFF_API_BASE_URL/login/end" \
-H "origin: $WEB_BASE_URL" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-b $LOGIN_COOKIES_FILE \
-d $PAGE_URL_JSON \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '400' ]; then
  echo "*** Posting a malicious code and state into the browser did not fail as expected"
  exit
fi
JSON=$(tail -n 1 $RESPONSE_FILE) 
echo $JSON | jq
CODE=$(jq -r .code <<< "$JSON")
if [ "$CODE" != 'invalid_request' ]; then
   echo "*** End login returned an unexpected error code"
   exit
fi
echo '8. Posting a malicious code and state into the browser was handled correctly'

#
# Test an authenticated page load by sending up the main cookies
#
echo '9. Testing an authenticated page load ...'
HTTP_STATUS=$(curl -k -i -s -X POST "$BFF_API_BASE_URL/login/end" \
-H "origin: $WEB_BASE_URL" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-b $MAIN_COOKIES_FILE \
-d '{"pageUrl":"'$WEB_BASE_URL'"}' \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '200' ]; then
  echo "*** Authenticated page load failed with status $HTTP_STATUS"
  exit
fi
JSON=$(tail -n 1 $RESPONSE_FILE)
echo $JSON | jq
CSRF=$(jq -r .csrf <<< "$JSON")
IS_LOGGED_IN=$(jq -r .isLoggedIn <<< "$JSON")
HANDLED=$(jq -r .handled <<< "$JSON")
if [ "$IS_LOGGED_IN" != 'true'  ] || [ "$HANDLED" != 'false' ]; then
   echo "*** End login returned an unexpected payload"
   exit
fi
echo '9. Authenticated page reload was successful'

#
# Test getting user info with an invalid origin
#
echo '10. Testing GET User Info from an untrusted origin ...'
HTTP_STATUS=$(curl -k -i -s -X GET "$BFF_API_BASE_URL/userInfo" \
-H "origin: https://malicious-site.com" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-o $RESPONSE_FILE -w '%{http_code}')
echo "$HTTP_STATUS"
if [ "$HTTP_STATUS" != '403' ]; then
  echo '*** Invalid user info request did not fail as expected'
  exit
fi
echo '10. GET User Info request for an untrusted origin was handled correctly'

#
# Test getting user info without a cookie
#
echo '11. Testing GET User Info without secure cookies ...'
HTTP_STATUS=$(curl -k -i -s -X GET "$BFF_API_BASE_URL/userInfo" \
-H "origin: $WEB_BASE_URL" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '401' ]; then
  echo '*** Invalid user info request did not fail as expected'
  exit
fi
JSON=$(tail -n 1 $RESPONSE_FILE) 
echo $JSON | jq
CODE=$(jq -r .code <<< "$JSON")
if [ "$CODE" != 'session_expired' ]; then
   echo "*** User Info returned an unexpected error code"
   exit
fi
echo '11. GET User Info request without secure cookies was handled correctly'

#
# Test getting user info successfully
#
echo '12. Testing GET User Info with secure cookies ...'
HTTP_STATUS=$(curl -k -i -s -X GET "$BFF_API_BASE_URL/userInfo" \
-H "origin: $WEB_BASE_URL" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-b $MAIN_COOKIES_FILE \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '200' ]; then
  echo "*** Getting user info failed with status $HTTP_STATUS"
  exit
fi
JSON=$(tail -n 1 $RESPONSE_FILE) 
echo $JSON | jq
echo "12. GET User Info was successful"

#
# Test refreshing a token with an invalid origin
#
echo '13. Testing POST to /refresh from an untrusted origin ...'
HTTP_STATUS=$(curl -k -i -s -X POST "$BFF_API_BASE_URL/refresh" \
-H "origin: https://malicious-site.com" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '403' ]; then
  echo '*** Invalid token refresh request did not fail as expected'
  exit
fi
echo '13. POST to /refresh for an untrusted origin was handled correctly'

#
# Test refreshing a token without a cookie
#
echo '14. Testing POST to /refresh without secure cookies ...'
HTTP_STATUS=$(curl -k -i -s -X POST "$BFF_API_BASE_URL/refresh" \
-H "origin: $WEB_BASE_URL" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '401' ]; then
  echo '*** Invalid token refresh request did not fail as expected'
  exit
fi
JSON=$(tail -n 1 $RESPONSE_FILE) 
echo $JSON | jq
CODE=$(jq -r .code <<< "$JSON")
if [ "$CODE" != 'unauthorized_request' ]; then
   echo "*** Refresh returned an unexpected error code"
   exit
fi
echo '14. POST to /refresh without secure cookies was handled correctly'

#
# Test refreshing a token with secure cookies but with a missing anti forgery token
#
echo '15. Testing POST to /refresh without CSRF token ...'
HTTP_STATUS=$(curl -k -i -s -X POST "$BFF_API_BASE_URL/refresh" \
-H "origin: $WEB_BASE_URL" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-b $MAIN_COOKIES_FILE \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '401' ]; then
  echo '*** Invalid token refresh request did not fail as expected'
  exit
fi
JSON=$(tail -n 1 $RESPONSE_FILE) 
echo $JSON | jq
CODE=$(jq -r .code <<< "$JSON")
if [ "$CODE" != 'unauthorized_request' ]; then
   echo "*** Refresh returned an unexpected error code"
   exit
fi
echo '15. POST to /refresh without CSRF token was handled correctly'

#
# Test refreshing a token with secure cookies but with an incorrect anti forgery token
#
echo '16. Testing POST to /refresh with incorrect CSRF token ...'
HTTP_STATUS=$(curl -k -i -s -X POST "$BFF_API_BASE_URL/refresh" \
-H "origin: $WEB_BASE_URL" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-H 'x-example-csrf: abc123' \
-b $MAIN_COOKIES_FILE \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '401' ]; then
  echo '*** Invalid token refresh request did not fail as expected'
  exit
fi
JSON=$(tail -n 1 $RESPONSE_FILE) 
echo $JSON | jq
CODE=$(jq -r .code <<< "$JSON")
if [ "$CODE" != 'unauthorized_request' ]; then
   echo "*** Refresh returned an unexpected error code"
   exit
fi
echo '16. POST to /refresh with incorrect CSRF token was handled correctly'

#
# Test refreshing a token, which will rewrite up to 3 cookies
#
echo '17. Testing POST to /refresh with correct secure details ...'
HTTP_STATUS=$(curl -k -i -s -X POST "$BFF_API_BASE_URL/refresh" \
-H "origin: $WEB_BASE_URL" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-H "x-example-csrf: $CSRF" \
-b $MAIN_COOKIES_FILE \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '204' ]; then
  echo "*** Refresh request failed with status $HTTP_STATUS"
  JSON=$(tail -n 1 $RESPONSE_FILE) 
  echo $JSON | jq
  exit
fi
echo '17. POST to /refresh with correct secure details completed successfully'

#
# Test refreshing a token again, to ensure that the new refresh token is used for the refresh
#
echo '18. Testing POST to /refresh with rotated refresh token ...'
HTTP_STATUS=$(curl -k -i -s -X POST "$BFF_API_BASE_URL/refresh" \
-H "origin: $WEB_BASE_URL" \
-H "origin: $WEB_BASE_URL" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-H "x-example-csrf: $CSRF" \
-b $MAIN_COOKIES_FILE \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '401' ]; then
  echo "*** Refresh request failed with status $HTTP_STATUS"
  JSON=$(tail -n 1 $RESPONSE_FILE) 
  echo $JSON | jq
  exit
fi
echo '18. POST to /refresh with rotated refresh token completed successfully'

#
# Test logging out with an invalid origin
#
echo '19. Testing logout POST with invalid web origin ...'
HTTP_STATUS=$(curl -k -i -s -X POST "$BFF_API_BASE_URL/logout" \
-H "origin: https://malicious-site.com" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-d '{"pageUrl":"'$WEB_BASE_URL'"}' \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '403' ]; then
  echo '*** Invalid logout request did not fail as expected'
  exit
fi
echo '19. POST to logout with an invalid web origin was successfully rejected'

#
# Test logging out without a cookie
#
echo '20. Testing logout POST without secure cookies ...'
HTTP_STATUS=$(curl -k -i -s -X POST "$BFF_API_BASE_URL/logout" \
-H "origin: $WEB_BASE_URL" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-d '{"pageUrl":"'$WEB_BASE_URL'"}' \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '401' ]; then
  echo '*** Invalid logout request did not fail as expected'
  exit
fi
JSON=$(tail -n 1 $RESPONSE_FILE)
echo $JSON | jq
CODE=$(jq -r .code <<< "$JSON")
if [ "$CODE" != 'unauthorized_request' ]; then
   echo "*** Logout returned an unexpected error code"
   exit
fi
echo '20. POST to logout without secure cookies was successfully rejected'

#
# Test logging out without an incorrect anti forgery token
#
echo '21. Testing logout POST with incorrect anti forgery token ...'
HTTP_STATUS=$(curl -k -i -s -X POST "$BFF_API_BASE_URL/logout" \
-H "origin: $WEB_BASE_URL" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-H "x-example-csrf: abc123" \
-b $MAIN_COOKIES_FILE \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '401' ]; then
  echo '*** Invalid logout request did not fail as expected'
  exit
fi
JSON=$(tail -n 1 $RESPONSE_FILE)
echo $JSON | jq
CODE=$(jq -r .code <<< "$JSON")
if [ "$CODE" != 'unauthorized_request' ]; then
   echo "*** Logout returned an unexpected error code"
   exit
fi
echo '21. POST to logout with incorrect anti forgery token was successfully rejected'

#
# Test getting the logout URL and clearing cookies successfully
#
echo '22. Testing logout POST with correct secure details ...'
HTTP_STATUS=$(curl -k -i -s -X POST "$BFF_API_BASE_URL/logout" \
-H "origin: $WEB_BASE_URL" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-H "x-example-csrf: $CSRF" \
-b $MAIN_COOKIES_FILE \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '200' ]; then
  echo "*** Logout request failed with status $HTTP_STATUS"
  exit
fi
echo '22. POST to logout with correct secure details completed successfully'
JSON=$(tail -n 1 $RESPONSE_FILE)
echo $JSON | jq

#
# Test following the end session redirect to sign out in the Curity Identity Server
#
echo '23. Testing following the end session redirect redirect ...'
END_SESSION_REQUEST_URL=$(jq -r .url <<< "$JSON")
HTTP_STATUS=$(curl -k -i -s -X GET $END_SESSION_REQUEST_URL \
-c $CURITY_COOKIES_FILE \
-o $RESPONSE_FILE -w '%{http_code}')
if [ $HTTP_STATUS != '303' ]; then
  echo "*** Problem encountered during an OpenID Connect end session redirect, status: $HTTP_STATUS"
  exit
fi
echo '23. End session redirect completed successfully'

#
# Test sending malformed JSON which currently results in a 500 error
#
echo '24. Testing sending malformed JSON to the BFF API ...'
HTTP_STATUS=$(curl -k -i -s -X POST "$BFF_API_BASE_URL/login/end" \
-H "origin: $WEB_BASE_URL" \
-H 'content-type: application/json' \
-H 'accept: application/json' \
-d 'XXX' \
-o $RESPONSE_FILE -w '%{http_code}')
if [ "$HTTP_STATUS" != '400' ]; then
  echo '*** Posting malformed JSON did not fail as expected'
  exit
fi
echo '24. Malformed JSON was handled in the expected manner'
