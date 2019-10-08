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

        stage ('build') {
            steps {
                notifyBitbucket commitSha1: '', considerUnstableAsSuccess: false, credentialsId: '', disableInprogressNotification: false, ignoreUnverifiedSSLPeer: false, includeBuildNumberInKey: false, prependParentProjectKey: false, projectKey: '', stashServerBaseUrl: ''
                sh './gradlew clean build --build-cache'
            }
        }

        stage ('archive') {
            steps {
                sh './gradlew publishToMavenLocal --build-cache'
            }
        }
    }

    post {
        always {
            junit '**/build/test-results/test/TEST-*.xml'
            notifyBitbucket commitSha1: '', considerUnstableAsSuccess: false, credentialsId: '', disableInprogressNotification: false, ignoreUnverifiedSSLPeer: false, includeBuildNumberInKey: false, prependParentProjectKey: false, projectKey: '', stashServerBaseUrl: ''
        }
        success {
            archiveArtifacts artifacts: '*/build/libs/*.jar'
        }
    }
}
