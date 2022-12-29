FROM azul/zulu-openjdk-debian:17.0.4-jre

WORKDIR /usr/api
COPY build/libs/oauth-agent-0.0.1-SNAPSHOT.jar /usr/api/

RUN adduser --disabled-password --home /home/apiuser --gecos '' apiuser
USER apiuser

CMD ["java", "-jar", "/usr/api/oauth-agent-0.0.1-SNAPSHOT.jar"]
