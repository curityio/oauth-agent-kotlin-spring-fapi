FROM azul/zulu-openjdk-alpine:11.0.12-jre

# Copy in resources
WORKDIR /usr/api
COPY build/libs/oauth-agent-0.0.1-SNAPSHOT.jar /usr/api/
RUN apk --no-cache add curl

# Configure a low privilege user
RUN addgroup -g 1001 apigroup
RUN adduser -u 1001 -G apigroup -h /home/apiuser -D apiuser

# The docker-compose.yml file configures SSL trust and then runs the API