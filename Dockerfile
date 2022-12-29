FROM azul/zulu-openjdk-debian:17.0.4-jre

# Copy in resources
WORKDIR /usr/api
COPY build/libs/oauth-agent-0.0.1-SNAPSHOT.jar /usr/api/

# Configure a low privilege user
RUN adduser --disabled-password --home /home/apiuser --gecos '' apiuser

# The docker-compose.yml file configures SSL trust and then runs the API