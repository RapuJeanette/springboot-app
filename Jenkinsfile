pipeline {
  agent any
  environment {
    STAGING_SERVER = 'root@spring-docker-demo'
    IMAGE_NAME = "demo-0.0.1-SNAPSHOT.jar"
  }
  stages {
    stage('Clone Repository') {
      steps {
        git 'https://github.com/RapuJeanette/springboot-app.git'
      }
    }
    stage('Build') {
      steps {
        bat 'mvn clean package -DskipTests'
      }
    }
    stage('Code Quality') {
      steps {
        bat 'mvn checkstyle:check'
      }
    }
    stage('Test') {
      steps {
        bat 'mvn test'
       }
    }
    stage('Code Coverage') {
      steps {
        bat "mvn jacoco:report"
      }
    }
    stage('Deploy to Staging') {
      steps {
        bat 'scp target/${ARTIFACT_NAME} $STAGING_SERVER:/var/local/staging/'
        bat 'ssh $STAGING_SERVER "nohup java -jar /var/local/staging/${ARTIFACT_NAME} > /dev/null 2>&1 &"'
      }
    }
    stage('Validate Deployment') {
      steps {
          bat 'sleep 10'
          bat 'curl --fail http://spring-docker-demo:8080/health'
      }
    }
  }
  post {
    always {
      junit '**/target/surefire-reports/*.xml'
      archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
    }
  }
}
