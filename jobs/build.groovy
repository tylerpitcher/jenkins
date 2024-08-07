pipeline {
  agent any
  stages {
    stage('SCM') {
      steps {
         // Clone repository using the repository parameter
        git url: "${params.repository}", branch: "main"
      }
    }
    stage('Build Image(s)') {
      steps {
        script {
          // Find all Dockerfile paths in the repository
          def dockerfiles = sh(script: 'find . -name Dockerfile', returnStdout: true).trim().split('\n')

          for (dockerfile in dockerfiles) {
            // Extract the path from the Dockerfile path
            def dir = dockerfile.replaceFirst(/\/Dockerfile$/, '')

            // Define the name of image
            def imageName = dir
              .replaceFirst(/^\.\//, "${params.serviceName}-")
              .replaceFirst(/^\./, "${params.serviceName}")

            // Print the image name being built
            echo "Building Image: ${imageName}"
            
            withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
              // Build the image using the directory path and image name
              def app = docker.build("${USERNAME}/${imageName}", "${dir}")

              // Push the image to the registry with a tag corresponding to the build number
              docker.withRegistry('https://registry.hub.docker.com', 'dockerhub') {
                app.push("${env.BUILD_NUMBER}")
              }
            }
          }
        }
      }
    }
  }
  post {
    always {
      cleanWs()
    }
  }
}