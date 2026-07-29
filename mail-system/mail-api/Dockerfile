FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY mail-system/mail-api/pom.xml ./pom.xml
RUN mvn -q -DskipTests dependency:go-offline
COPY mail-system/mail-api/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S mailapi && adduser -S -G mailapi mailapi
WORKDIR /app
COPY --from=build /workspace/target/mail-api-*.jar /app/mail-api.jar
USER mailapi
EXPOSE 8090
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/mail-api.jar"]
