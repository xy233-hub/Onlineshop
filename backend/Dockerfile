# 使用本地JDK镜像运行项目
FROM eclipse-temurin:17-jdk
VOLUME /tmp
COPY target/onlineshop-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]

