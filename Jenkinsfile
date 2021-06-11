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
    	DEPLOY_MASTER_UPDATE_SITE = "/apps/knime/web/${KNIME_VERSION}/update/nibr"
    	DEPLOY_BRANCH_UPDATE_SITE = "/apps/knime/web/${KNIME_VERSION}/update/knime-rdkit-review"
    	QUALIFIER_PREFIX = "vnibr"
    }

    stages {
	    stage('GitCheckout') {
	        steps {
	        	checkout scm
				dir("scripts") {
					// TODO: Change to refs/heads/master
          			checkout([$class: 'GitSCM', branches: [[name: 'refs/heads/KNIME-1023_Setup_maven_as_build_tool']], doGenerateSubmoduleConfigurations: false, \
          			extensions: [], gitTool: 'default-git', submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket-jenkins', \
          			url: "https://bitbucket.prd.nibr.novartis.net/scm/knim/knime-build-scripts.git"]]])
        		}
	        }
	    }    
        stage('Compile and Build') {
        	steps {
				// Output environment
				sh "env"
	
	            // Compiles the plugin and builds an update site from it
		        configFileProvider([configFile(fileId: 'artifactory-maven-settings', variable: 'MAVEN_SETTINGS')]) {
					sh(label: "Compile and Build", script: "mvn -U clean verify -Dupdate.site=${UPDATE_SITE} -Dqualifier.prefix=${QUALIFIER_PREFIX} -s ${MAVEN_SETTINGS}")
		        }
		    }    
        }
        stage('Running Tests') {
        	steps {
				// Output environment
				sh '''
					cd "${WORKSPACE}"
					source ./scripts/community.inc			
					export LC_NUMERIC=en_US.UTF-8
					export RELEASE_REPOS="${UPDATE_SITE}"
					runTests file://${WORKSPACE}/org.rdkit.knime.update/target/repository Testflows/${JOB_NAME##*-}/RDKit "${WORKSPACE}/org.rdkit.knime.testing/regression-tests/zips"
				'''
        	}
            post {
				// Archive always the test results
                success {
                    junit 'results/**/*.xml'
                }
            }
        	
        } 
        stage('Deploying to Update Site') {
			steps {
		        script {
					if (env.git_branch_lowercase == 'master' || env.GIT_BRANCH == 'master') {
						
					} 
					else {
						// Deploy resulting build artifacts as review update (overriding an existing one)
						sh '''
							rm -rf "${DEPLOY_BRANCH_UPDATE_SITE}/${BRANCH_NAME}"
							mv "${WORKSPACE}/org.rdkit.knime.update/target/repository" "${DEPLOY_BRANCH_UPDATE_SITE}/${BRANCH_NAME}"
						'''
					}
		        }
		    }
        } 
    }
}
