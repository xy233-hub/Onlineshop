# dockerfile
FROM eclipse-temurin:17-jdk
WORKDIR /app

# 复制项目文件
COPY . /app

# 安装Maven
RUN apt-get update && apt-get install -y maven

# 编译项目并生成可执行jar包
RUN mvn clean package -DskipTests

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

# 查看生成的jar包
RUN ls -la target/

# 使用 jar 文件运行应用
ENTRYPOINT ["sh","-c","java -jar target/*.jar --media.upload-dir=${MEDIA_UPLOAD_DIR}"]