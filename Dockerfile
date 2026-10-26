# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# 运行阶段
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /build/target/chat-server-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
