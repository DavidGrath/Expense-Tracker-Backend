//It is assumed that ../app-version is changed manually before running this
pipeline {
    environment {
        MYSQL_CREDS = credentials('mysql_credentials')
    }
    agent {
        node {
            label 'Test Server'
        }
    }
    // Branch dev
    stages {

        stage('Flyway') {

            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew setUpCIDb'
                dir 'database/flyway'
                sh 'flyway migrate -environment=dev  -user=$MYSQL_CREDS_USR -password=$MYSQL_CREDS_PSW'
            }
        }
        stage('JooQ') {
            steps {
                sh './gradlew jooqCodeGenCi'
            }
        }
        stage('Open API') {
            steps {
                sh './gradlew openApiGenerate'
            }
        }
        stage('Build') {
            steps {
                sh './gradlew assemble'
            }
        }
        stage('Test') {
            environment {
                SPRING_PROFILES_ACTIVE='ci'
            }
            steps {
                sh './gradlew test'
            }
        }
        stage('Ansible setup') {
            steps {
                dir 'ansible'
                sh 'ansible-playbook playbooks/setup_service.yaml'
            }
        }
        stage('Deploy') {
            steps {
                sh './gradlew bootJar'
                sh 'scp ./app/build/libs/expense-tracker-backend.jar jenkins@172.26.48.26:/opt/expense-tracker/expense-tracker-backend.jar'
                sh 'sudo systemctl daemon-reload'
                sh 'sudo systemctl restart expense-tracker'
            }
        }
    }
}