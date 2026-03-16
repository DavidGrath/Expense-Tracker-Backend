// Branch staging

// Upload JAR to Artifactory
// Ansible setup service on test_server
// Download jar to /opt/expense-tracker
// Systemctl start/restart service

//It is assumed that ../app-version is changed manually before running this
pipeline {
    environment {
        FLYWAY_STAGING = credentials('flyway-credentials-staging')
        FLYWAY_DEV = credentials('flyway-credentials-dev')
        MYSQL_CREDS = credentials('mysql-credentials-dev')
    }
    agent {
        node {
            label 'Test Server'
        }
    }
    // Branch staging
    stages {

        stage('Clean') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew clean'
            }
        }
        stage('Flyway') {
            steps {
                dir ('database/flyway') {
                    sh 'flyway migrate -environment=dev  -user=$FLYWAY_DEV_USR -password=$FLYWAY_DEV_PSW'
                }
            }
        }
        stage('JooQ') {
            steps {
                sh './gradlew jooqCodeGenDev'
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
                sh 'rm -f app/src/main/resources/secrets-ci.properties'
                sh 'chmod o+w -R app'
            withCredentials([file(credentialsId: 'expense-tracker-backend-secrets-ci', variable: 'expenseTrackerApplicationSecrets')]) {
                sh 'cp $expenseTrackerApplicationSecrets app/src/main/resources/secrets-ci.properties'
            }
                sh './gradlew test'
            }
        }
        stage('Flyway Staging') {
            agent {
                node {
                    label 'Staging Server'
                }
            }
            steps {
                dir ('database/flyway') {
                    sh 'flyway migrate -environment=staging  -user=$FLYWAY_STAGING_USR -password=$FLYWAY_STAGING_PSW'
//                     sh 'flyway migrate -environment=staging'
                }
            }
        }
        stage('Ansible setup') {
            agent {
                node {
                    label 'built-in'
                }
            }
            environment {
                ANSIBLE_BECOME_PASS = credentials('ansible-become-password')
            }
            steps {
                dir ('ansible') {
                    sh 'ansible-playbook --limit staging_server playbooks/setup_service.yaml -e "ansible_become_password=\'$ANSIBLE_BECOME_PASS\'"'
                }
            }
        }
        stage('Copy Jar') {
            agent {
                node {
                    label 'built-in'
                }
            }
            steps {
                sh './gradlew bootJar'
                sh 'scp -i /etc/ssh/jenkins_remote "./app/build/libs/expense-tracker-backend.jar" jenkins_remote@172.26.48.26:/opt/expense_tracker/expense-tracker-backend.jar'
            }
        }
        stage('Deploy') {
            agent {
                node {
                    label 'Staging Server'
                }
            }
            steps {
                sh 'cp app-version /opt/expense_tracker/app-version'
                sh 'chmod 777 /opt/expense_tracker/expense-tracker-backend.jar'
                withCredentials([file(credentialsId: 'expense-tracker-backend-secrets-staging', variable: 'expenseTrackerApplicationSecrets')]) {
                    sh 'sudo rm -f /opt/expense_tracker/secrets.properties'
                    sh 'cp $expenseTrackerApplicationSecrets /opt/expense_tracker/secrets.properties'
                    sh 'chmod 700 /opt/expense_tracker/secrets.properties'
                    sh 'sudo chown root:root /opt/expense_tracker/secrets.properties'
                }
                sh 'sudo systemctl enable expense_tracker_backend'
                sh 'sudo systemctl daemon-reload'
                sh 'sudo systemctl restart expense_tracker_backend'
                sh 'sudo systemctl status expense_tracker_backend'
            }
        }
    }
    post {
        always {
            junit 'app/build/test-results/**/*.xml'
        }
    }
}