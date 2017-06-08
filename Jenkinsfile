
pipeline {
    agent label:"docker", docker: "maven:3.3.9-jdk-8"

    environment {
      MAVEN_OPTS = "-Xmx1024m"
    }

    stages {
        stage("Build") {
            sh 'mvn package -B -Dmaven.test.failure.ignore=true'
        }
    }

    postBuild {
        success {
            archive "**/target/**/*.jar,**/target/**/*.hpi"
            junit '**/target/surefire-reports/*.xml'
        }
    }

}
