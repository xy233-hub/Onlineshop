pipeline {
    agent any

    // 定义环境变量
    environment {
        // 镜像名称 (不需要仓库前缀，直接本地用)
        IMAGE_BACKEND = 'onlineshop-backend'
        IMAGE_FRONTEND = 'onlineshop-frontend'
        // 镜像标签
        IMAGE_TAG = "${BUILD_NUMBER}"

        // 项目在服务器上的部署目录
        // Jenkins 会把 docker-compose.yml 复制到这里并执行
        DEPLOY_PATH = '/data/onlineshop'
    }

    tools {
        // 请确保 Jenkins 全局配置里的名字和这里一致
        jdk 'JDK17'
        maven 'Maven3.6'
        nodejs 'NodeJS'
    }

    stages {
       stage('Checkout') {
            steps {
                echo '=== 1. 拉取代码（脚本指定仓库） ==='
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/test']],  // 要拉取的分支
                    userRemoteConfigs: [[
                        url: 'git@github.com:xy233-hub/Onlineshop.git',  // 仓库URL
                        credentialsId: 'jenkins-SSH'  // 你在Jenkins中创建的凭证ID
                    ]],
                ])
            }
        }
        stage('Build Backend Jar') {
            steps {
                echo '=== 2. 编译 Spring Boot ==='
                dir('backend') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Build Frontend Dist') {
            steps {
                echo '=== 3. 构建 Vue ==='
                dir('frontend/shop-front') {
                    sh 'npm install'
                    sh 'npm run build'
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                echo '=== 4. 构建本地 Docker 镜像 ==='
                script {
                    // 构建后端镜像 (直接在本地构建，不推送)
                    dir('backend') {
                        sh "docker build -t ${IMAGE_BACKEND}:latest ."

                    }

                    // 构建前端镜像
                    dir('frontend/shop-front') {
                        sh "docker build -t ${IMAGE_FRONTEND}:latest ."
                    }
                }
            }
        }

        stage('Deploy Local') {
            steps {
                echo '=== 5. 本地部署 (启动容器) ==='
                script {
                    // 确保部署目录存在
                    sh "mkdir -p ${DEPLOY_PATH}"
                    // 清空临时上传目录：/data/onlineshop/uploads/temp
                    sh """
                        if [ -d ${DEPLOY_PATH}/uploads/temp ]; then
                          rm -rf ${DEPLOY_PATH}/uploads/temp/*
                        fi
                    """
                    // 将 docker-compose.yml 复制到部署目录
                    // 注意：这里假设 docker-compose.yml 在项目根目录
                    sh "cp docker-compose.yml ${DEPLOY_PATH}/"
                    sh "cp text1.2.sql ${DEPLOY_PATH}/"
                    // 进入目录并启动
                    dir("${DEPLOY_PATH}") {
                        // 传递环境变量给 docker-compose
                        sh "export IMAGE_TAG=latest && docker compose down --remove-orphans"
                        sh "export IMAGE_TAG=latest && docker compose up -d"
                    }
                }
            }
        }
    }

    post {
        success {
            echo '部署成功！访问 http://你的服务器IP 试试'
            // 可选：清理一下旧的悬空镜像
            sh 'docker image prune -f'
        }
        failure {
            echo '部署失败！'
        }
    }
}