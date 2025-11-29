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
    // Optionnel: nom d'une installation Maven configurée dans Jenkins (laisser vide pour utiliser mvn du PATH)
    string(name: 'MAVEN_TOOL', defaultValue: '', description: 'Nom de l installation Maven configurée dans Jenkins (optionnel)')
    // Optionnel: nom d'une installation JDK configurée dans Jenkins (laisser vide pour utiliser le JDK système)
    string(name: 'JDK_TOOL', defaultValue: '', description: 'Nom de l installation JDK configurée dans Jenkins (optionnel)')
  }

  environment {
    MVN_FLAGS = "-B"
    REPO_URL  = "https://github.com/joeladjidan/codingame-java17.git"
    // NOTE: créez un credential Jenkins (Username with password) contenant votre nom GitHub
    // comme username et un Personal Access Token (PAT) comme password, puis mettez son ID ci-dessous
    GIT_CREDENTIALS_ID = 'github-token'
  }

  stages {
    stage('Checkout') {
      steps {
        // Utilise les credentials Jenkins pour le checkout HTTPS (évite l'erreur d'authentification)
        checkout([$class: 'GitSCM', branches: [[name: "refs/heads/${params.BRANCH}"]],
                  userRemoteConfigs: [[url: env.REPO_URL, credentialsId: env.GIT_CREDENTIALS_ID]]])
      }
    }

    stage('Build') {
      steps {
        script {
          // Si l'utilisateur a fourni un outil Maven configuré dans Jenkins, on l'utilise.
          if (params.MAVEN_TOOL?.trim()) {
            try {
              def mvnHome = tool name: params.MAVEN_TOOL, type: 'maven'
              env.PATH = "${mvnHome}/bin;${env.PATH}"
              echo "Using Maven tool '${params.MAVEN_TOOL}' at ${mvnHome}"
            } catch (err) {
              echo "Maven tool '${params.MAVEN_TOOL}' not found in Jenkins configuration, falling back to system 'mvn'"
            }
          } else {
            echo "No MAVEN_TOOL provided; using 'mvn' from PATH"
          }

          // Si l'utilisateur a fourni un JDK configuré dans Jenkins, on l'utilise.
          if (params.JDK_TOOL?.trim()) {
            try {
              def javaHome = tool name: params.JDK_TOOL, type: 'jdk'
              env.JAVA_HOME = javaHome
              env.PATH = "${javaHome}/bin;${env.PATH}"
              echo "Using JDK tool '${params.JDK_TOOL}' at ${javaHome}"
            } catch (err) {
              echo "JDK tool '${params.JDK_TOOL}' not found in Jenkins configuration, falling back to system Java"
            }
          } else {
            echo "No JDK_TOOL provided; using system Java"
          }

          def skipArg = params.SKIP_TESTS ? '-DskipTests=true' : ''
          def mavenCommand = "${env.MVN_FLAGS} ${params.MVN_GOALS} ${skipArg}"
          if (isUnix()) {
            sh "mvn ${mavenCommand}"
          } else {
            bat "mvn ${mavenCommand}"
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
        archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
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
