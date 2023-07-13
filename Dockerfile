FROM azul/zulu-openjdk-debian:17-jre-latest

WORKDIR /usr/api
COPY build/libs/oauth-agent-0.0.1-SNAPSHOT.jar /usr/api/

# Trust certificates in the example deployment - remove this in a real deployment
RUN keytool -import -alias example.ca -cacerts -file /usr/api/certs/example.ca.pem -storepass changeit -noprompt

RUN groupadd --gid 10000 apiuser \
  && useradd --uid 10001 --gid apiuser --shell /bin/bash --create-home apiuser
USER 10001

CMD ["java", "-jar", "/usr/api/oauth-agent-0.0.1-SNAPSHOT.jar"]