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
        }
        success {
            echo '✅ All tests PASSED!'
        }
        failure {
            echo '❌ Some tests FAILED — check the reports.'
        }
    }
}
