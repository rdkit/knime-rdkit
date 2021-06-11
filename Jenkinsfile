pipeline {
    agent {
        node {
            label 'knime-test-basel-c7'
        }
    }

    environment {
    	M2_HOME = "/apps/knime/buildtools/apache-maven"
		PATH = "${M2_HOME}/bin:${PATH}"
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
		        configFileProvider([configFile(fileId: 'artifactory-maven-settings', variable: 'MAVEN_SETTINGS')]) {
	              sh(label: "Compile and Build", script: "mvn -U clean verify -Dupdate.site=${UPDATE_SITE} -Dqualifier.prefix=${QUALIFIER_PREFIX} -s ${MAVEN_SETTINGS}")
		        }
		    }    
        }
        stage('Installing Test Instance') {
        	steps {
				// Output environment
				sh "env"
        	}
        } 
        stage('Running Tests') {
        	steps {
				// Output environment
				sh "env"
        	}
        } 
        stage('Deploying to Update Site') {
			steps {
		        script {
		          if (env.git_branch_lowercase == 'master' || env.GIT_BRANCH == 'master') {
	
		          } 
		          else {
	
		          }
		        }
		    }
        } 
    }
}
