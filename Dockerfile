# Use an open source Java 11 base image
FROM azul/zulu-openjdk-alpine:11.0.12-jre

# Copy in the main JAR file
WORKDIR /usr/api
COPY build/libs/backend-for-frontend-0.0.1-SNAPSHOT.jar /usr/api/

# Copy in SSL certs

# Configure SSL trust
# COPY certs/root.ca.pem /usr/local/share/ca-certificates/trusted.ca.pem
# RUN keytool -keystore /usr/lib/jvm/zulu11-ca/lib/security/cacerts -storepass changeit -importcert -alias internalroot -file /usr/local/share/ca-certificates/root.ca.pem -noprompt

# Configure a low privilege user
CMD ["java", "-jar", "/usr/api/backend-for-frontend-0.0.1-SNAPSHOT.jar"]