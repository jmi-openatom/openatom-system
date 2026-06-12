FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace/lab-management-system

COPY lab-management-system/ ./
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy

ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Shanghai \
    JAVA_OPTS=""

WORKDIR /app
RUN groupadd --system lab && useradd --system --gid lab --home /app lab
COPY --from=build /workspace/lab-management-system/lab-admin/target/lab-admin-*.jar /app/app.jar
RUN chown -R lab:lab /app

USER lab
EXPOSE 8091
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
