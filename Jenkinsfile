pipeline {
    agent any

    environment {
        IMAGE_BACKEND = 'onlineshop-backend'
        IMAGE_FRONTEND = 'onlineshop-frontend'
        IMAGE_TAG = "${BUILD_NUMBER}"
        DEPLOY_PATH = '/data/onlineshop'
    }

    tools {
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
                    branches: [[name: '*/test']],
                    userRemoteConfigs: [[
                        url: 'git@github.com:xy233-hub/Onlineshop.git',
                        credentialsId: 'jenkins-SSH'
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



        stage('Build Docker Images') {
            steps {
                echo '=== 3. 构建本地 Docker 镜像 ==='
                script {
                    dir('backend') {
                        sh "docker build -t ${IMAGE_BACKEND}:latest ."
                    }
                    dir('frontend/shop-front') {
                        sh "docker build -t ${IMAGE_FRONTEND}:latest ."
                    }
                }
            }
        }

        stage('Deploy Local') {
            steps {
                echo '=== 4. 本地部署 (启动容器) ==='
                script {
                    sh "mkdir -p ${DEPLOY_PATH}"
                    sh """
                        if [ -d ${DEPLOY_PATH}/uploads/temp ]; then
                          rm -rf ${DEPLOY_PATH}/uploads/temp/*
                        fi
                    """
                    sh "cp docker-compose.yml ${DEPLOY_PATH}/"
                    sh "cp text1.2.sql ${DEPLOY_PATH}/"
                    dir("${DEPLOY_PATH}") {
                        sh "export IMAGE_TAG=latest && docker compose down --remove-orphans"
                        sh "export IMAGE_TAG=latest && docker compose up -d"
                    }
                }
            }
        }

        stage('API Tests (Allure)') {
            steps {
                echo '=== 5. 运行 API 自动化测试并生成 Allure 结果 ==='
                dir('ecommerce-api-test') {
                    // 这里假设项目已配置 allure-maven 插件或 surefire 写入 allure-results
                    sh 'mvn clean test'
                }
            }
            post {
                always {
                    // 归档 surefire 报告（可选，但建议）
                    junit allowEmptyResults: true, testResults: 'ecommerce-api-test/target/surefire-reports/*.xml'
                }
            }
        }
    }

    post {
        always {
            // 发布 Allure 报告：要求 Jenkins 已安装 Allure Jenkins 插件，并配置好 Allure Commandline
            allure([
                includeProperties: false,
                jdk: '',
                reportBuildPolicy: 'ALWAYS',
                results: [[path: 'ecommerce-api-test/target/allure-results']]
            ])
        }
        success {
            echo '部署成功！访问 http://你的服务器IP 试试'
            sh 'docker image prune -f'
        }
        failure {
            echo '部署失败！'
        }
    }
}
