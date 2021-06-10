pipeline {
    agent {
        node {
            label 'knime-test-basel-c7'
        }
    }

    environment {
    	KNIME_VERSION = "4.3"
    	UPDATE_SITE = "http://chbs-knime-app.tst.nibr.novartis.net/${KNIME_VERSION}/update/mirror"
    	QUALIFIER_PREFIX = "vnibr"
    }

    stages {
        stage('Compile and Build') {
            steps {
                // Get code from BitBucket repository
                checkout scm

				// Output environment
				sh "env"

                // Compiles the plugin and builds an update site from it
                sh "mvn -U clean verify -Dupdate.site=${UPDATE_SITE} -Dqualifier.prefix=$QUALIFIER_PREFIX"
            }
        }
    }
}
