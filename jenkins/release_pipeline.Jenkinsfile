#!groovy

pipeline {

    triggers {
        pollSCM('H/2 * * * *')
        bitbucketPush()
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder (logRotator (numToKeepStr: '10', artifactNumToKeepStr: '10'))
        timestamps()
    }

    agent any

    stages {

        stage ('release') {
            steps {
                sh './gradlew clean release --build-cache'
            }
        }
    }

    post {
        always {
            script {
                currentBuild.description = "${env.GIT_COMMIT.substring(0,7)}"
                currentBuild.result = currentBuild.result ?: 'SUCCESS'
            }
        }
        success {
            archiveArtifacts artifacts: '*/build/libs/*.jar'
        }
    }
}
