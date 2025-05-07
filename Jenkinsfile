// pipeline {
//     agent any
//     stages {
//         stage('Clean') {
//             steps {
//                 sh 'mvn clean'
//             }
//         }
//         stage('Compile') {
//             steps {
//                 sh 'mvn compile'
//             }
//         }
//         stage('Test') {
//             steps {
//                 sh 'mvn test -Dmaven.test.failure.ignore=true'
//             }
//         }
//         stage('PMD') {
//             steps {
//                 sh 'mvn pmd:pmd'
//             }
//         }
//         stage('JaCoCo') {
//             steps {
//                 sh 'mvn jacoco:report'
//             }
//         }
//         stage('Javadoc') {
//             steps {
//                 sh 'mvn javadoc:javadoc'
//             }
//         }
//         stage('Site') {
//             steps {
//                 sh 'mvn site'
//             }
//         }
//         stage('Package') {
//             steps {
//                 sh 'mvn package -DskipTests'
//             }
//         }
//     }
//     post {
//         always {
//         archiveArtifacts artifacts: '**/target/site/**/*.*', fingerprint: true
//         archiveArtifacts artifacts: '**/target/**/*.jar', fingerprint: true
//         archiveArtifacts artifacts: '**/target/**/*.war', fingerprint: true
//         junit '**/target/surefire-reports/*.xml'
//         }
//     }
// }

pipeline {
    agent any
    environment {
        DOCKER_HUB_CREDENTIALS = credentials('dockerhub_credentials')
        DOCKER_IMAGE = '12345wtx/teedy-app'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }
    stages {
        stage('Clean') {
            steps { sh 'mvn clean' }
        }
        stage('Compile') {
            steps { sh 'mvn compile' }
        }
        stage('Test') {
            steps { sh 'mvn test -Dmaven.test.failure.ignore=true' }
        }
        stage('PMD') {
            steps { sh 'mvn pmd:pmd' }
        }
        stage('JaCoCo') {
            steps { sh 'mvn jacoco:report' }
        }
        stage('Javadoc') {
            steps { sh 'mvn javadoc:javadoc' }
        }
        stage('Site') {
            steps { sh 'mvn site' }
        }
        stage('Package') {
            steps { sh 'mvn package -DskipTests' }
        }
        stage('Building image') {
            steps {
                script {
                    docker.build("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}")
                }
            }
        }
        stage('Upload image') {
            steps {
                script {
                    docker.withRegistry('https://registry.hub.docker.com', env.DOCKER_HUB_CREDENTIALS) {
                        docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push()
                        docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push('latest')
                    }
                }
            }
        }
        stage('Run containers') {
            steps {
                script {
                    sh 'docker stop teedy-container-8081 || true'
                    sh 'docker rm teedy-container-8081 || true'
                    docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").run(
                        '--name teedy-container-8081 -d -p 8081:8080'
                    )
                    sh 'docker ps --filter "name=teedy-container"'
                }
            }
        }
    }
    post {
        always {
            node {
                archiveArtifacts artifacts: '**/target/site/**/*.*', fingerprint: true
                archiveArtifacts artifacts: '**/target/**/*.jar', fingerprint: true
                archiveArtifacts artifacts: '**/target/**/*.war', fingerprint: true
                junit '**/target/surefire-reports/*.xml'
            }
        }
    }
}
