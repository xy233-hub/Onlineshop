# dockerfile
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 复制已构建的 jar（在宿主用 mvn package 生成）
COPY target/*.jar app.jar

# 运行时可通过环境变量覆盖上传目录和 base url
ENV MEDIA_UPLOAD_DIR=/app/uploads \
    MEDIA_BASE_URL=http://localhost:8080/media

# 创建目录并创建/设置非 root 用户
RUN mkdir -p ${MEDIA_UPLOAD_DIR} \
    && addgroup --system app \
    && adduser --system --ingroup app app \
    && chown -R app:app ${MEDIA_UPLOAD_DIR}

USER app

VOLUME ["/app/uploads"]
EXPOSE 8080

# 将路径改为 /app/app.jar（因为 WORKDIR 是 /app 且 jar 复制为 app.jar）
ENTRYPOINT ["sh","-c","java -jar /app/app.jar --media.upload-dir=${MEDIA_UPLOAD_DIR}"]
