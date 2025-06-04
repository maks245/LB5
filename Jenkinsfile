pipeline {
    agent { label 'docker-minikube-agent' }

    tools {
        jdk 'JDK-21'
    }

    environment {
        IMAGE_NAME = "lb5:latest"
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                git branch: 'master', url: 'https://github.com/maks245/LB5.git'
            }
        }

        stage('Build') {
            steps {
                echo 'Building the project...'
                withEnv(["JAVA_HOME=${tool 'JDK-21'}"]) {
                    sh 'bash ./mvnw clean compile'
                }
            }
        }

        stage('Test') {
            steps {
                echo 'Running tests...'
                withEnv(["JAVA_HOME=${tool 'JDK-21'}"]) {
                    sh 'bash ./mvnw test'
                }
            }
            post {
                always {
                    echo 'Publishing test results...'
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging the application...'
                withEnv(["JAVA_HOME=${tool 'JDK-21'}"]) {
                    sh 'bash ./mvnw package -DskipTests'
                }
            }
        }

        stage('Archive Artifacts') {
            steps {
                echo 'Archiving artifacts...'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Build Docker Image') {
            steps {
                echo 'Building Docker image...'
                script {
                    sh 'eval $(minikube -p minikube docker-env) && docker build -t $IMAGE_NAME .'
                }
            }
        }

        stage('Deploy to Minikube') {
            steps {
                echo 'Deploying to Minikube...'
                script {
                    // Застосування Kubernetes маніфестів
                    sh 'kubectl apply -f k8s/deployment.yaml'
                    sh 'kubectl apply -f k8s/service.yaml'

                    // Очікуємо розгортання
                    timeout(time: 5, unit: 'MINUTES') {
                        sh 'kubectl rollout status deployment/lb5-deployment --watch=true'
                    }

                    // Отримати URL (опціонально)
                    sh 'minikube service lb5-service --url'
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline successfully completed!'
        }
        failure {
            echo 'Pipeline failed.'
        }
        always {
            echo 'Cleaning workspace...'
            cleanWs()
        }
    }
}
