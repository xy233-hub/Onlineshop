# 项目部署指南

本指南将帮助您通过 Docker 部署 OnlineShop 项目到本地环境。

## 前提条件

在开始部署之前，请确保您的系统满足以下要求：

1. **安装 Docker**：请从 [Docker 官网](https://www.docker.com/get-started) 下载并安装最新版本的 Docker Desktop。
2. **安装 Docker Compose**：Docker Desktop 通常会自带 Docker Compose。
3. **网络连接**：确保您的网络可以访问 Docker Hub 或配置了可靠的镜像源。

## 部署步骤

### 步骤 1：克隆项目

如果您还没有克隆项目，请先克隆到本地：

```bash
git clone <项目仓库地址>
cd Onlineshop-feature-D
```

### 步骤 2：构建和运行项目

使用 Docker Compose 构建和运行整个项目：

```bash
# 进入项目根目录
cd Onlineshop-feature-D

# 构建并运行所有服务
docker-compose up -d
```

### 步骤 3：验证部署

部署完成后，您可以通过以下方式验证服务是否正常运行：

1. **前端服务**：访问 `http://localhost`
2. **后端服务**：访问 `http://localhost:8081`
3. **数据库**：MySQL 服务运行在容器内部，端口为 3306

## 服务说明

项目包含三个主要服务：

### 1. MySQL 数据库 (`db`)
- **镜像**：`mysql:8.0`
- **端口**：容器内部 3306
- **环境变量**：
  - `MYSQL_ROOT_PASSWORD`: 123456
  - `MYSQL_DATABASE`: onlineshop
  - `MYSQL_USER`: shopuser
  - `MYSQL_PASSWORD`: shoppass
- **数据持久化**：使用本地 `mysql_data` 目录
- **初始化**：使用 `text1.2.sql` 脚本初始化数据库

### 2. 后端服务 (`backend`)
- **构建**：基于 `backend` 目录的 Dockerfile
- **端口**：`8081:8080`
- **依赖**：依赖于 `db` 服务
- **环境变量**：
  - `SPRING_DATASOURCE_URL`: 连接到 MySQL 数据库
  - `SPRING_DATASOURCE_USERNAME`: shopuser
  - `SPRING_DATASOURCE_PASSWORD`: shoppass
  - `MEDIA_UPLOAD_DIR`: /app/uploads
  - `MEDIA_BASE_URL`: http://localhost:8081/media
- **数据持久化**：使用本地 `uploads` 目录存储上传的媒体文件

### 3. 前端服务 (`frontend`)
- **构建**：基于 `frontend/shop-front` 目录的 Dockerfile
- **端口**：`80:80`
- **依赖**：依赖于 `backend` 服务

## 常见问题及解决方案

### 1. 镜像拉取失败

**问题**：无法拉取 Docker 镜像，出现网络错误。

**解决方案**：
- 检查网络连接
- 配置 Docker 镜像源，例如：
  - 阿里云：`https://<your-id>.mirror.aliyuncs.com`
  - 中科大：`https://docker.mirrors.ustc.edu.cn`
  - 网易：`https://hub-mirror.c.163.com`

### 2. 后端构建失败

**问题**：后端服务构建失败，找不到 `target/onlineshop-0.0.1-SNAPSHOT.jar` 文件。

**解决方案**：
- 确保您已经安装了 Maven
- 在 `backend` 目录中运行 `mvn clean package -DskipTests` 构建项目
- 或者使用修改后的 Dockerfile，它会在容器中自动构建项目

### 3. 数据库初始化失败

**问题**：MySQL 数据库初始化失败，无法执行 `text1.2.sql` 脚本。

**解决方案**：
- 确保 `text1.2.sql` 文件存在于项目根目录
- 检查 SQL 脚本是否格式正确
- 尝试手动执行 SQL 脚本初始化数据库

### 4. 服务启动顺序问题

**问题**：后端服务在数据库准备就绪前启动，导致连接失败。

**解决方案**：
- Docker Compose 的 `depends_on` 只是确保服务启动顺序，不保证服务就绪
- 可以在后端服务中添加重试机制，或者使用 `wait-for-it.sh` 脚本

## 管理服务

### 查看服务状态

```bash
docker-compose ps
```

### 查看服务日志

```bash
# 查看所有服务日志
docker-compose logs

# 查看特定服务日志
docker-compose logs backend
```

### 停止服务

```bash
docker-compose down
```

### 重新构建服务

```bash
docker-compose build
```

## 注意事项

1. **数据持久化**：项目使用本地目录存储数据库数据和上传的媒体文件，请确保这些目录有适当的权限。
2. **环境变量**：根据您的实际环境，可能需要调整环境变量，特别是 `MEDIA_BASE_URL`。
3. **网络配置**：如果您的系统有防火墙，请确保相关端口（80、8081）已开放。
4. **性能优化**：对于生产环境，建议调整 MySQL 和应用服务的配置以获得更好的性能。

## 技术栈

- **前端**：Vue 3 + Element Plus
- **后端**：Spring Boot + MyBatis
- **数据库**：MySQL 8.0
- **容器化**：Docker + Docker Compose

---

祝您部署顺利！如果遇到任何问题，请参考上述解决方案或联系技术支持。