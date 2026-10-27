pipeline {
  // any avaibale agent
  agent any

  options {
    skipDefaultCheckout(true)
    // Prevent concurrent builds of the same job
    disableConcurrentBuilds()
  }

  stages {
    stage('Checkout') {
      steps {
        echo 'Checking out the repository.'
        checkout scm
      }
    }
    stage('Build and test User Service') {
      steps {
        echo 'Building and testing the User Service.'
        dir('backend/userservice') {
          sh 'sh ./mvnw -B test'
        }
      }
    }

    stage('Build and test Product Service') {
      steps {
        echo 'Building and testing the Product Service.'
        dir('backend/productservice') {
          sh 'sh ./mvnw -B test'
        }
      }
    }

    stage('Build and test Media Service') {
      steps {
        echo 'Building and testing the Media Service.'
        dir('backend/mediaservice') {
          sh 'sh ./mvnw -B test'
        }
      }
    }

    stage('Install frontend dependencies') {
      steps {
        echo 'Installing frontend dependencies.'
        dir('frontend') {
          sh 'npm ci'
        }
      }
    }

    stage('Build frontend') {
      steps {
        echo 'Building the frontend.'
        dir('frontend') {
          sh 'npm run build'
        }
      }
    }

    stage('Test frontend') {
      steps {
        echo 'Running frontend tests.'
        dir('frontend') {
          sh 'CI=true npm test'
        }
      }
    }

    stage('SonarQube analysis') {
      steps {
        echo 'Analyzing code with SonarQube.'
        script {
          def scannerHome = tool 'SonarScanner'
          withSonarQubeEnv('SonarQube') {
            sh "${scannerHome}/bin/sonar-scanner"
          }
          timeout(time: 10, unit: 'MINUTES') {
          waitForQualityGate abortPipeline: true
        }
        }
      }
    }

    // stage('Quality gate') {
    //   steps {
    //     echo 'Waiting for the SonarQube quality gate.'
    //     timeout(time: 10, unit: 'MINUTES') {
    //       waitForQualityGate abortPipeline: true
    //     }
    //   }
    // }

    stage('Deploy application') {
      steps {
        echo 'Building and deploying the app'
        sh '''
          set -eu

          state_file='/var/jenkins_home/buy-01-deployed-image-tag'
          image_tag="build-${BUILD_NUMBER}"
          previous_tag='latest'

          if [ -f "$state_file" ]; then
            previous_tag=$(cat "$state_file")
          fi

          echo "Building release $image_tag."
          if ! IMAGE_TAG="$image_tag" docker compose --project-name buy-01 --file docker-compose.yml build; then
            echo 'Image build failed. The existing deployment is still running.'
            exit 1
          fi

          echo "Deploying release $image_tag to the existing buy-01 Compose project."
          if IMAGE_TAG="$image_tag" docker compose --project-name buy-01 --file docker-compose.yml up -d --no-build --wait --wait-timeout 300; then
            echo "$image_tag" > "$state_file.tmp"
            mv "$state_file.tmp" "$state_file"
            echo 'Deployment completed successfully.'
          else
            echo "Deployment failed. Restoring release $previous_tag."
            if IMAGE_TAG="$previous_tag" docker compose --project-name buy-01 --file docker-compose.yml up -d --no-build --wait --wait-timeout 300; then
              echo 'Rollback completed successfully.'
            else
              echo 'Rollback failed. Inspect the Docker Compose logs.'
            fi
            exit 1
          fi
        '''
      }
    }
  }

  post {
        always {
        echo 'Publishing test reports.'

        junit(
            testResults: '**/target/surefire-reports/TEST-*.xml,frontend/test-results/frontend-test-results.xml',
            allowEmptyResults: false
        )

        archiveArtifacts(
            artifacts: '**/target/surefire-reports/**,frontend/test-results/**',
            allowEmptyArchive: true
        )
    }
    success {
        echo 'Pipeline completed successfully.'

        mail(
            to: env.NOTIFICATION_EMAIL,
            subject: "Jenkins SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            body: """
            Build Status: SUCCESS

            Job: ${env.JOB_NAME}
            Build Number: ${env.BUILD_NUMBER}

            Your pipeline completed successfully.
            """
        )
    }

    unsuccessful {
        echo 'Pipeline failed. Check the console output for details.'

        mail(
            to: env.NOTIFICATION_EMAIL,
            subject: "Jenkins FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            body: """
            Build Status: FAILURE

            Job: ${env.JOB_NAME}
            Build Number: ${env.BUILD_NUMBER}

            Check the Jenkins console output to investigate the failure.
            """
        )
    }
  }
}