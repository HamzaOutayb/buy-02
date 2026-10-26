FROM maven:3.9.16-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN if [ ! -f src/main/resources/gateway.p12 ]; then \
      keytool -genkeypair \
        -alias gateway \
        -keyalg RSA \
        -keysize 2048 \
        -storetype PKCS12 \
        -keystore src/main/resources/gateway.p12 \
        -validity 365 \
        -dname "CN=localhost" \
        -ext "SAN=dns:localhost,ip:127.0.0.1" \
        -storepass changeit \
        -noprompt; \
    fi
RUN mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
