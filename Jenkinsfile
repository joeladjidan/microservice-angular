// groovy
pipeline {
  agent any

  options {
    buildDiscarder(logRotator(daysToKeepStr: '14'))
    timeout(time: 1, unit: 'HOURS')
    timestamps()
  }

  parameters {
    string(name: 'BRANCH', defaultValue: 'main', description: 'Branche Git à builder')
    string(name: 'MVN_GOALS', defaultValue: 'clean verify', description: 'Goals Maven à exécuter')
    booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Ignorer les tests')
    string(name: 'MAVEN_TOOL', defaultValue: '', description: 'Nom de l installation Maven configurée dans Jenkins (optionnel)')
    string(name: 'JDK_TOOL', defaultValue: '', description: 'Nom de l installation JDK configurée dans Jenkins (optionnel)')
    booleanParam(name: 'BUILD_FRONTEND', defaultValue: true, description: 'Construire le frontend Angular (si présent)')
    string(name: 'NODE_TOOL', defaultValue: '', description: 'Nom de l installation NodeJS configurée dans Jenkins (optionnel)')
    string(name: 'NPM_CREDENTIAL_ID', defaultValue: '', description: 'ID credential (Secret text) contenant le token NPM (optionnel)')
    string(name: 'FRONTEND_DIR', defaultValue: 'frontend', description: 'Répertoire du projet frontend relatif à la racine')
  }

  environment {
    MVN_FLAGS = "-B"
    REPO_URL  = "https://github.com/joeladjidan/microservice-angular.git"
    GIT_CREDENTIALS_ID = 'github-token'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout([$class: 'GitSCM', branches: [[name: "refs/heads/${params.BRANCH}"]],
                  userRemoteConfigs: [[url: env.REPO_URL, credentialsId: env.GIT_CREDENTIALS_ID]]])
      }
    }

    stage('Setup Tools') {
      steps {
        script {
          if (params.MAVEN_TOOL?.trim()) {
            try {
              def mvnHome = tool name: params.MAVEN_TOOL, type: 'maven'
              env.PATH = "${mvnHome}/bin;${env.PATH}"
              echo "Using Maven tool '${params.MAVEN_TOOL}' at ${mvnHome}"
            } catch (err) {
              echo "Maven tool '${params.MAVEN_TOOL}' not found, using system 'mvn'"
            }
          } else {
            echo "No MAVEN_TOOL provided; using 'mvn' from PATH"
          }

          if (params.JDK_TOOL?.trim()) {
            try {
              def javaHome = tool name: params.JDK_TOOL, type: 'jdk'
              env.JAVA_HOME = javaHome
              env.PATH = "${javaHome}/bin;${env.PATH}"
              echo "Using JDK tool '${params.JDK_TOOL}' at ${javaHome}"
            } catch (err) {
              echo "JDK tool '${params.JDK_TOOL}' not found, using system Java"
            }
          } else {
            echo "No JDK_TOOL provided; using system Java"
          }

          if (params.NODE_TOOL?.trim()) {
            try {
              def nodeHome = tool name: params.NODE_TOOL, type: 'nodejs'
              env.PATH = "${nodeHome}/bin;${env.PATH}"
              echo "Using Node tool '${params.NODE_TOOL}' at ${nodeHome}"
            } catch (err) {
              echo "Node tool '${params.NODE_TOOL}' not found in Jenkins configuration, using system Node/npm"
            }
          } else {
            echo "No NODE_TOOL provided; using system Node/npm"
          }
        }
      }
    }

    stage('Frontend Build') {
      when {
        expression { return params.BUILD_FRONTEND }
      }
      steps {
        script {
          def frontendDir = params.FRONTEND_DIR?.trim() ?: 'frontend'
          if (!fileExists(frontendDir)) {
            echo "Frontend directory `${frontendDir}` not found, skipping frontend build"
          } else {
            if (params.NPM_CREDENTIAL_ID?.trim()) {
              withCredentials([string(credentialsId: params.NPM_CREDENTIAL_ID, variable: 'NPM_TOKEN')]) {
                try {
                  if (isUnix()) {
                    sh """
                      set -e
                      cd ${frontendDir}
                      printf "//registry.npmjs.org/:_authToken=\${NPM_TOKEN}\n" > .npmrc
                    """
                  } else {
                    bat """
                      cd ${frontendDir}
                      echo //registry.npmjs.org/:_authToken=%NPM_TOKEN% > .npmrc
                    """
                  }
                  if (isUnix()) {
                    sh """
                      set -e
                      cd ${frontendDir}
                      if [ -f package-lock.json ]; then
                        npm ci --prefer-offline
                      else
                        npm install --prefer-offline
                      fi
                      npm run build --if-present
                    """
                  } else {
                    bat """
                      cd ${frontendDir}
                      if exist package-lock.json (
                        npm ci --prefer-offline
                      ) else (
                        npm install --prefer-offline
                      )
                      call npm run build --if-present
                    """
                  }
                } finally {
                  // Cleanup .npmrc to avoid leaking token
                  if (isUnix()) {
                    sh "cd ${frontendDir} || exit 0; rm -f .npmrc"
                  } else {
                    bat "cd ${frontendDir} && if exist .npmrc del /q .npmrc"
                  }
                }
              }
            } else {
              echo "No NPM_CREDENTIAL_ID set; assuming public registry or preconfigured auth"
              if (isUnix()) {
                sh """
                  set -e
                  cd ${frontendDir}
                  if [ -f package-lock.json ]; then
                    npm ci --prefer-offline
                  else
                    npm install --prefer-offline
                  fi
                  npm run build --if-present
                """
              } else {
                bat """
                  cd ${frontendDir}
                  if exist package-lock.json (
                    npm ci --prefer-offline
                  ) else (
                    npm install --prefer-offline
                  )
                  call npm run build --if-present
                """
              }
            }
          }
        }
      }
    }

    stage('Build Backend (Maven)') {
      steps {
        script {
          def skipArg = params.SKIP_TESTS ? '-DskipTests=true' : ''
          def mavenCommand = "${env.MVN_FLAGS} ${params.MVN_GOALS} ${skipArg}"

          // Detect pom.xml location; if absent, search recursively
          def mvnDir = '.'
          if (!fileExists('pom.xml')) {
            def poms = findFiles(glob: '**/pom.xml')
            if (poms.length == 0) {
              error "Aucun `pom.xml` trouvé dans l'espace de travail. La build Maven est annulée."
            }
            mvnDir = poms[0].path - 'pom.xml'
            if (mvnDir == '') { mvnDir = '.' }
          }
          echo "Running Maven in: ${mvnDir}"

          dir(mvnDir) {
            if (isUnix()) {
              sh "mvn ${mavenCommand}"
            } else {
              bat "mvn ${mavenCommand}"
            }
          }
        }
      }
    }

    stage('Publish Test Results') {
      steps {
        junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
      }
    }

    stage('Archive') {
      steps {
        script {
          archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
          def frontendDistGlob = "${params.FRONTEND_DIR}/dist/**"
          if (fileExists(params.FRONTEND_DIR)) {
            try {
              archiveArtifacts artifacts: frontendDistGlob, fingerprint: true
            } catch (e) {
              echo "No frontend dist artifacts to archive (pattern: ${frontendDistGlob})"
            }
          } else {
            echo "Frontend directory `${params.FRONTEND_DIR}` not found, skipping artifact archiving"
          }
        }
      }
    }
  }

  post {
    always {
      junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
      cleanWs()
    }
    success {
      echo "Build succeeded for ${params.BRANCH}"
    }
    failure {
      echo "Build failed for ${params.BRANCH}"
    }
  }
}
