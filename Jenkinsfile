pipeline {
    agent any // Запускати пайплайн на будь-якому доступному агентові

    tools {
            jdk 'JDK-21' // ТОЧНО та сама назва, яку ви дали в Jenkins Tools для JDK
//             maven 'Maven-3.2.2'   // ТОЧНО та сама назва, яку ви дали в Jenkins Tools для Maven
        }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                // Отримання актуальної версії коду з вашого репозиторію Git.
                // Використовуйте checkout scm, якщо Jenkins Job налаштований на SCM.
                // Або конкретний URL:
                git branch: 'master', url: 'https://github.com/maks245/LB5.git'
                // Замініть 'your-username' та 'your-repository.git'
            }
        }

        stage('Build') {
            steps {
                echo 'Building the project...'
                 withEnv(["JAVA_HOME=${tool 'JDK-21'}"]) { // ВИКОРИСТОВУЙТЕ НАЗВУ ВАШОГО JDK З JENKINS UI
                                    sh 'bash ./mvnw clean compile'
                                }

            }
        }

        stage('Test') {
            steps {
                echo 'Running tests...'
                 withEnv(["JAVA_HOME=${tool 'JDK-21'}"]) { // ВИКОРИСТОВУЙТЕ НАЗВУ ВАШОГО JDK З JENKINS UI
                                    sh 'bash ./mvnw test'
                                }
            }
            post {
                always {
                    // Публікація результатів тестів JUnit (Maven Surefire Reports)
                    echo 'Publishing test results...'
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging the application...'
                 withEnv(["JAVA_HOME=${tool 'JDK-21'}"]) { // ВИКОРИСТОВУЙТЕ НАЗВУ ВАШОГО JDK З JENKINS UI
                                    sh 'bash ./mvnw package -DskipTests'
                                }
            }
        }

        // Опціональний, але рекомендований етап: Архівація зібраного JAR-файлу
        stage('Archive Artifacts') {
            steps {
                echo 'Archiving artifacts...'
                // Збереження зібраного JAR-файлу в Jenkins
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        // Опціональний етап: Збірка Docker-образу (для підвищення складності)
        // Цей етап вимагає доступу Jenkins до Docker демона хоста
        stage('Build Docker Image') {
            steps {
                echo 'Building Docker image...'
                script {
                    // Отримуємо версію з pom.xml або іншим способом
                    // Вам може знадобитися читати pom.xml за допомогою readMavenPom
                    // Або передавати версію як параметр
                    def appVersion = "1.0.0-SNAPSHOT" // Замініть на реальну версію або динамічне отримання
                    docker.build("LB5:${appVersion}", ".")
                    // Замініть 'your-username/your-app' на ім'я вашого образу
                }
            }
        }

        // Опціональний етап: Пуш Docker-образу в реєстр (наприклад, Docker Hub)
        // stage('Push Docker Image') {
        //     steps {
        //         echo 'Pushing Docker image to registry...'
        //         script {
        //             def appVersion = "1.0.0-SNAPSHOT" // Версія з попереднього етапу
        //             // Потрібні Jenkins Credentials для Docker Hub (ID: docker-hub-creds)
        //             withCredentials([usernamePassword(credentialsId: 'docker-hub-creds', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
        //                 docker.withRegistry('https://registry.hub.docker.com', 'docker-hub-creds') {
        //                     docker.image("your-username/your-app:${appVersion}").push()
        //                 }
        //             }
        //         }
        //     }
        // }
    }

    // Дії після виконання всіх етапів
    post {
        success {
            echo 'Pipeline successfully completed!'
        }
        failure {
            echo 'Pipeline failed. Check console output for details.'
            // Можна додати відправку сповіщень (наприклад, Slack, Email)
            // mail to: 'your-email@example.com',
            //      subject: "Jenkins Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            //      body: "Build URL: ${env.BUILD_URL}"
        }
        aborted {
            echo 'Pipeline was aborted.'
        }
        unstable {
            echo 'Pipeline completed with unstable results (e.g., some tests failed).'
        }
        always {
            echo 'Pipeline finished.'
            // Очищення робочого простору (корисно для зменшення місця на диску)
            cleanWs()
        }
    }
}