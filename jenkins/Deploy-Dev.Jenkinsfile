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

        stage('Clean') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew clean'
            }
        }
        stage('Flyway') {

            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew setUpCIDb'
                dir ('database/flyway') {
                    sh 'flyway migrate -environment=ci  -user=$MYSQL_CREDS_USR -password=$MYSQL_CREDS_PSW'
                }
            }
        }
        //TODO Version Provider
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
                sh 'rm -f app/src/main/resources/secrets-ci.properties'
                sh 'chmod o+w -R app'
            withCredentials([file(credentialsId: 'expense-tracker-backend-secrets-ci', variable: 'expenseTrackerApplicationSecrets')]) {
                sh 'cp $expenseTrackerApplicationSecrets app/src/main/resources/secrets-ci.properties'
            }
                sh './gradlew test'
            }
        }
        stage('Flyway Dev') {
            steps {
                dir ('database/flyway') {
                    sh 'flyway migrate -environment=dev  -user=$MYSQL_CREDS_USR -password=$MYSQL_CREDS_PSW'
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
                    sh 'ansible-playbook -i inventory playbooks/setup_service.yaml  --private-key ~/.ssh/jenkins_agent_key -e "ansible_become_password=\'$ANSIBLE_BECOME_PASS\'"'
                }
            }
        }
        stage('Deploy') {
            steps {
                sh './gradlew bootJar'
                sh 'cp "./app/build/libs/expense-tracker-backend.jar" /opt/expense_tracker/expense-tracker-backend.jar'
                sh 'cp app-version /opt/expense_tracker/app-version'
                sh 'chmod 777 /opt/expense_tracker/expense-tracker-backend.jar'
                withCredentials([file(credentialsId: 'expense-tracker-backend-secrets-dev', variable: 'expenseTrackerApplicationSecrets')]) {
                    sh 'sudo rm -f /opt/expense_tracker/secrets-dev.properties'
                    sh 'cp $expenseTrackerApplicationSecrets /opt/expense_tracker/secrets-dev.properties'
                    sh 'chmod 700 /opt/expense_tracker/secrets-dev.properties'
                    sh 'sudo chown root:root /opt/expense_tracker/secrets-dev.properties'
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