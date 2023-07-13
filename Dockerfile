FROM azul/zulu-openjdk-debian:17-jre-latest

WORKDIR /usr/api
COPY build/libs/oauth-agent-0.0.1-SNAPSHOT.jar /usr/api/

RUN groupadd --gid 10000 apiuser \
  && useradd --uid 10001 --gid apiuser --shell /bin/bash --create-home apiuser
USER 10001

# The example docker compose command runs the API after configuring SSL trust
#CMD ["java", "-jar", "/usr/api/oauth-agent-0.0.1-SNAPSHOT.jar"]