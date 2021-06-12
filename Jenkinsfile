pipeline {
    agent {
        node {
        	// This job needs to run on the KNIME server that hosts also the target update site 
            label 'knime-test-basel-c7'
        }
    }

    environment {		
		// A feature (branch or master) should always be built for one specific KNIME version only
    	KNIME_VERSION = "4.3"

    	// Two pre-requisites that need to be installed by the NIBR Jenkins job knime4.x-all-setup-build-environment
    	DIRECTOR_HOME = "/apps/knime/buildtools/director"
    	M2_HOME = "/apps/knime/buildtools/apache-maven"
		PATH = "${M2_HOME}/bin:${PATH}"
		
		// Scripts required for testing and deployment
		GIT_REPO_SCRIPTS = "https://bitbucket.prd.nibr.novartis.net/scm/knim/knime-build-scripts.git"
		// TODO: Change to refs/heads/master
        GIT_BRANCH_SCRIPTS = "refs/heads/KNIME-1023_Setup_maven_as_build_tool"
        
        # Configuration for KNIME test instance
        GIT_REPO_CONFIG = "https://bitbucket.prd.nibr.novartis.net/scm/knim/knime-client-config.git"
    	GIT_BRANCH_CONFIG = "${KNIME_VERSION}"
    	
    	// Prefix for the version number of the artifacts to distinguish them from normal community builds 
    	// (vnibrYYYYMMDDHHSS always is considered a higher version than a community built version vYYYYMMDDHHSS)
    	QUALIFIER_PREFIX = "vnibr"
    	
    	// Source update site used for building the KNIME Test Instance for regression testing
    	UPDATE_SITE = "http://chbs-knime-app.tst.nibr.novartis.net/${KNIME_VERSION}/update/mirror"
    	
    	// Define extra IUs to be installed for running test workflows
    	EXTRA_IUs = "org.rdkit.knime.feature.feature.group," 
    	
    	// Target update sites to use when everything was tested successfully to deploy the build artifacts
    	DEPLOY_MASTER_UPDATE_SITE = "/apps/knime/web/${KNIME_VERSION}/update/nibr"
    	DEPLOY_BRANCH_UPDATE_SITE = "/apps/knime/web/${KNIME_VERSION}/update/knime-rdkit-review"
    }

    stages {
	    stage('GitCheckout') {
	        steps {
	        	// Get the branch that triggered this build 
	        	checkout scm
	        	
	        	// In addition get scripts required for testing and deployment
	        	// They will be checked out into a scripts directory, which resides within the root directory of the main checkout,
	        	// which is sub-optimal, but no other solution was found so far
				dir("scripts") {
					checkout([$class: 'GitSCM', branches: [[name: "${GIT_BRANCH_SCRIPTS}"]], doGenerateSubmoduleConfigurations: false, \
          			extensions: [], gitTool: 'default-git', submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket-jenkins', \
          			url: "${GIT_REPO_SCRIPTS}"]]])
        		}
				dir("config") {
					checkout([$class: 'GitSCM', branches: [[name: "${GIT_BRANCH_CONFIG}"]], doGenerateSubmoduleConfigurations: false, \
          			extensions: [], gitTool: 'default-git', submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket-jenkins', \
          			url: "${GIT_REPO_CONFIG}"]]])
        		}
	        }
	    }   
	    stage('Cleanup') {
        	steps {
				// Output environment
				sh "env"
				
				// Cleanup old data from last build
				sh "rm -rf tmp results mirrorWorkspace"
			}
		}	 
        stage('Compile and Build') {
        	steps {
	            // Compiles the plugin and builds an update site from it
		        configFileProvider([configFile(fileId: 'artifactory-maven-settings', variable: 'MAVEN_SETTINGS')]) {
					sh(label: "Compile and Build", script: "mvn -U clean verify -Dupdate.site=${UPDATE_SITE} -Dqualifier.prefix=${QUALIFIER_PREFIX} -s ${MAVEN_SETTINGS}")
		        }
		    }    
        }
        stage('Running Tests') {
        	steps {
				// Installs with the Director tool a new minimal KNIME instance with the build artifacts (needs to run as bash script!)
				// The logic comes from the KNIME Community implementation (community.inc), which was slightly adapted to disable
				// accessing the KNIME Community server for test workflows, because we provide those as part of the RDKit testing feature
				// and cannot access the server from NIBR (proxy settings and credentials would be required for authentication)
				sh '''#!/bin/bash
					cd "${WORKSPACE}"
					ln -sf "${DIRECTOR_HOME}" "./scripts/knime-community/director"
					# Apply NIBR KNIME configuration of DEV environment
					source ./scripts/knime-community/community.inc
					configureKnimeTestInstance() {
    					local knimeFolder=$1
    					./scripts/applyConfig.sh "${knimeFolder}" ./config dev linux7 chbs IgnoreKnimeIni
					}
					export LC_NUMERIC=en_US.UTF-8
					export RELEASE_REPOS="${UPDATE_SITE}"
					runTests file://${WORKSPACE}/org.rdkit.knime.update/target/repository "noServerAccess" "${WORKSPACE}/org.rdkit.knime.testing/regression-tests/zips"
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
						// Add successfully tested RDKit artifacts to existing NIBR update site
						sh '''
							"${WORKSPACE}/scripts/mirrorSingleUpdateSite.sh" "${WORKSPACE}/tmp/knime test/knime" "${DEPLOY_MASTER_UPDATE_SITE}" true true "${WORKSPACE}/scripts/mirror.xml" "${WORKSPACE}/org.rdkit.knime.update/target/repository/"
						'''
					} 
					else {
						// Deploy resulting build artifacts of branches as review update with the branch name only (overriding an existing one)
						sh '''
							rm -rf "${DEPLOY_BRANCH_UPDATE_SITE}/${BRANCH_NAME}"
							mkdir -p "${DEPLOY_BRANCH_UPDATE_SITE}/${BRANCH_NAME}"
							mv -f "${WORKSPACE}/org.rdkit.knime.update/target/repository"/* "${DEPLOY_BRANCH_UPDATE_SITE}/${BRANCH_NAME}/"
						'''
					}
		        }
		    }
        } 
    }
}
