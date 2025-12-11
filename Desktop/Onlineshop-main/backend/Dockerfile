# 以官方 OpenJDK 镜像为基础
FROM openjdk:17-slim
VOLUME /tmp
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]

