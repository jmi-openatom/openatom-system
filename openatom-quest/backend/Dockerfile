FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B --no-transfer-progress package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 quest
COPY --from=build /app/target/openatom-quest-0.1.0-SNAPSHOT.jar app.jar
USER quest
EXPOSE 8931
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
