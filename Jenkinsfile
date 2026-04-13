pipeline {
    agent any

    parameters {
        choice(
            name: 'TEST_SUITE',
            choices: ['all', 'gems', 'coins'],
            description: 'Which test suite to run'
        )
        string(
            name: 'BASE_URL',
            defaultValue: 'http://subscription-admin-backend-service-jwr-qa-8.jwrnonprod.int',
            description: 'Backend base URL for the target environment'
        )
    }

    environment {
        MAVEN_OPTS = '-Xmx1024m'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {
        // ADD THIS NEW STAGE FIRST
        stage('Clean Old Reports') {
            steps {
                // This deletes the contents of the reports and target folders
                // so old historical files don't pile up!
                sh 'rm -rf reports/* target/*'
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Compile') {
            steps {
                sh 'mvn clean compile -DskipTests'
            }
        }

        stage('Run Tests') {
            steps {
                script {
                    def testArg = ''
                    switch (params.TEST_SUITE) {
                        case 'gems':
                            testArg = '-Dtest="GemsPassE2ETest"'
                            break
                        case 'coins':
                            testArg = '-Dtest="CoinsPassE2ETest"'
                            break
                        default:
                            // runs full testng.xml (both suites)
                            testArg = ''
                            break
                    }
                    sh "mvn test ${testArg} -Dbase.url=${params.BASE_URL} -DfailIfNoTests=false"
                }
            }
        }
    }

    post {
        always {
            // 1. Grab your custom reports from the new folder
            archiveArtifacts artifacts: 'reports/*.html', allowEmptyArchive: true

            // Archive custom HTML reports
            archiveArtifacts artifacts: 'target/surefire-reports/*.html', allowEmptyArchive: true

            // Archive TestNG XML results
            archiveArtifacts artifacts: 'target/surefire-reports/**', allowEmptyArchive: true

            // Publish TestNG results (requires TestNG Results Plugin)
            junit testResults: 'target/surefire-reports/junitreports/*.xml', allowEmptyResults: true

            // Publish Allure report (requires Allure Jenkins Plugin)
            //allure includeProperties: false,
              //     jdk: '',
                 //  results: [[path: 'allure-results']]

        emailext(
                to: 'mohit.kumar@jungleegames.com', // Replace with real emails
                subject: "IAP API Automation Report: ${currentBuild.fullDisplayName} - ${currentBuild.currentResult}",
                body: """
                    <h3>IAP (Gems/Coins/Tournament Subscription)API Test Execution Complete</h3>
                    <p>Hello Team,</p>
                    <p>The automated IAP API tests for Coins, Gems, and Tournament Subscription have finished running.</p>
                    <p><strong>Final Status:</strong> ${currentBuild.currentResult}</p>
                    <p>Please find the detailed HTML test reports attached to this email.</p>
                    <p>You can also view the full build logs directly on Jenkins here: <br>
                    <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
                """,
                mimeType: 'text/html',
                attachmentsPattern: 'reports/*.html' // This grabs your custom reports!
            )         
        }
        success {
            echo '✅ All tests PASSED!'
        }
        failure {
            echo '❌ Some tests FAILED — check the reports.'
        }
    }
}
