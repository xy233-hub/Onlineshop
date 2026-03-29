# 使用多阶段构建，先构建项目
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN apt-get update && apt-get install -y maven
RUN mvn clean package -DskipTests

# 运行项目
FROM eclipse-temurin:17-jdk
VOLUME /tmp
COPY --from=builder /app/target/onlineshop-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]

