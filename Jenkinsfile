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
	    stage('GitCheckout') {
	        steps {
	            checkout \
	                scm: [ $class : 'GitSCM', \
	                     extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'org.rdkit']],
	                                 [ $class: 'ScmName', name: 'rdkit-git' ]], \
	                     userRemoteConfigs: [[ \
	                         url: 'https://bitbucket.prd.nibr.novartis.net/scm/knim/knime-rdkit.git'  \
	                     ]]
	                ]
	            ]
	            checkout \
	                scm: [ $class : 'GitSCM', \
	                     branches: [[name: 'refs/heads/KNIME-1023_Setup_maven_as_build_tool']], \
	                     extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'scripts']],
	                                 [ $class: 'ScmName', name: 'scripts' ]], \
	                     userRemoteConfigs: [[ \
	                         url: 'https://bitbucket.prd.nibr.novartis.net/scm/knim/knime-build-scripts.git'  \
	                     ]]
	                ]
	            ]
	        }
	    }    
        stage('Compile and Build') {
        	steps {
				// Output environment
				sh "env"
	
	            // Compiles the plugin and builds an update site from it
	            dir('org.rdkit') {
			        configFileProvider([configFile(fileId: 'artifactory-maven-settings', variable: 'MAVEN_SETTINGS')]) {
		              sh(label: "Compile and Build", script: "mvn -U clean verify -Dupdate.site=${UPDATE_SITE} -Dqualifier.prefix=${QUALIFIER_PREFIX} -s ${MAVEN_SETTINGS}")
			        }
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
