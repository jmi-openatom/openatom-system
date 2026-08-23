# Build stage
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/pom.xml ./
RUN mvn dependency:resolve -B
COPY backend/src ./src
RUN mvn clean package -DskipTests -B

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8921
# host 网络下 docker 无法用 mem_limit 约束容器，JVM 必须显式指定堆大小。
# 默认 1G 堆适配多副本部署（4G 服务器可跑 2 实例），可用 JAVA_OPTS 覆盖。
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:MaxMetaspaceSize=320m -XX:+UseG1GC -XX:MaxGCPauseMillis=100"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]
