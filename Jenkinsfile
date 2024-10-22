pipeline {
    agent {
        node {
            // This job needs to run on the KNIME server that hosts also the target update site 
            // The following values are possible: knime-dev-chbs-c7, knime-test-basel-c7
            label 'knime-test-basel-c7'
        }
    }
    
    options {
    	// Do not build the same branch twice at the same time
        disableConcurrentBuilds()
    }
    
    environment { 	
    	// Email addresses to be notified about failures, unstable tests or fixes
    	EMAIL_TO = 'manuel.schwarze@novartis.com'
    	
	// A feature (branch, master or master_nibr) should always be built for one specific KNIME version only
	KNIME_VERSION = "5.3"
    	
	// The Java version to be used to compile and build - possible values are java8, java11 and java17
	JAVA_VERSION = "java17"

	// Two pre-requisites that need to be installed by the Novartis Jenkins job knime4.x-all-setup-build-environment
	DIRECTOR_HOME = "/apps/knime/buildtools/director"
	M2_HOME = "/apps/knime/buildtools/apache-maven"
	PATH = "${M2_HOME}/bin:${PATH}"
	MAVEN_SETTINGS_FILE = "${WORKSPACE}/settings.xml"

	// OWASP Dependency Check settings
	// Installation folder (optional, will just not run if it does not exist)
	DC_HOME = "/apps/knime/buildtools/dependency-check"
	// CVSS score threshold that is considered a failure (with this value or above)
	CVSS_SCORE_THRESHOLD = 7
	// File with exceptions (false positives that we accept)		
	DC_SUPPRESSION_FILE = "${WORKSPACE}/owasp-suppressions.xml"
	
	// Scripts required for testing and deployment
	GIT_REPO_SCRIPTS = "https://bitbucketenterprise.aws.novartis.net/scm/knim/knime-build-scripts.git"
        GIT_BRANCH_SCRIPTS = "refs/heads/master"
    	
    	// Prefix for the version number of the artifacts to distinguish them from normal community builds 
    	// (vnibrYYYYMMDDHHSS always is considered a higher version than a community built version vYYYYMMDDHHSS)
    	QUALIFIER_PREFIX = "vnibr"
    	
    	// Source update site used for building the KNIME Test Instance for regression testing
    	UPDATE_SITE_TEMPLATE = "http://chbs-knime-app.%URL_SHORT_ENV%.nibr.novartis.net/${KNIME_VERSION}/update/mirror"
    	
    	// Define extra IUs to be installed for running test workflows (used by community.inc methods)
    	EXTRA_IUs = "org.knime.features.core.streaming.feature.group" 
    	
    	// Defines how tests are performed - this is passed in addition to the runTests method, which internally calls
    	// the KNIME org.knime.testing.NGTestflowRunner application, which comes with the org.knime.features.testing.application feature.
    	// To see an output of valid arguments like the following, specify an invalid argument.
    	// 
		// Valid arguments:
		//     -include <regex>: only tests matching the regular expression <regex> will be run. The complete path of each testcase starting from the testflows' root directory is matched, e.g. '/Misc/Workflow'.
		//     -root <dir_name>: optional, specifies the root dir where all testcases are located in. Multiple root arguments may be present.
		//     -server <uri>: optional, a KNIME server  from which workflows should be downloaded first. Has to be used with -serverPath.
		//                    Example: http://<user>:<password>@host[:port]/knime/rest
		//     -serverPath <path>: optional, a path on the KNIME Server that specifies which workflows should be downloaded first.
		//                    Example: /workflowGroup1/workflowGroup2
		//     -xmlResult <file_name>: specifies a single XML  file where the test results are written to.
		//     -xmlResultDir <directory_name>: specifies the directory  into which each test result is written to as an XML files. Either -xmlResult or -xmlResultDir must be provided.
		//     -outputToSeparateFile: optional, specifies that system out and system err are written to a separate text file instead of being included in the XML result file (similar to Surefire)
		//     -loadSaveLoad: optional, loads, saves, and loads the workflow before execution.
		//     -deprecated: optional, reports deprecated nodes in workflows as failures.
		//     -views: optional, opens all views during a workflow test.
		//     -dialogs: optional, additional tests all node dialogs.
		//     -logMessages: optional, checks for required or unexpected log messages.
		//     -ignoreNodeMessages: optional, ignores any warning messages on nodes.
		//     -untestedNodes <regex>: optional, checks for untested nodes, only node factories matching the regular expression are reported
		//     -save <directory_name>: optional, specifies the directory  into which each testflow is saved after execution. If not specified the workflows are not saved.
		//     -timeout <seconds>: optional, specifies the timeout for each individual workflow.
		//     -stacktraceOnTimeout: optional, if specified output a full stack trace in case of timeouts.
		//     -memLeaks <bytes>: optional, specifies the maximum allowed increases in heap usage for each testflow. If not specified no test for memory leaks is performed.
		//     -streaming: optional, enables additional streaming test for workflows configured accordingly. The test streaming job manager is set and used for each single node.
		//     -preferences <file_name>: optional, specifies an exported preferences file that should be used to initialize preferences
		//     -workflow.variable <variable-declaration>: optional, defines or overwrites workflow variable 'name' with value 'value' (possibly enclosed by quotes). The 'type' must be one of "String", "int" or "double".
    	TEST_DEPTH_PARAMS = "-loadSaveLoad -views -logMessages -streaming -stacktraceOnTimeout"
    	
    	// Target update sites to use when everything was tested successfully to deploy the build artifacts
    	MASTER_UPDATE_SITE_URL_TEMPLATE = "http://chbs-knime-app.%URL_SHORT_ENV%.nibr.novartis.net/${KNIME_VERSION}/update/nibr"
    	DEPLOY_MASTER_UPDATE_SITE = "/apps/knime/web/${KNIME_VERSION}/update/nibr"
    	DEPLOY_BRANCH_UPDATE_SITE = "/apps/knime/web/${KNIME_VERSION}/update/knime-rdkit-review"
    	
    	// Usually, this is set to "false", but in certain situations it can be necessary to deploy also branch builds to the "master" update site - then set it to "true"
    	DEPLOY_BRANCH_BUILDS_TO_MASTER = "false"
    }

    stages {
    	stage('Detect Env') {
    		steps {
    			script {
    				if (env.NODE_LABELS.toUpperCase().contains("DEV")) {
    					env.ENVIRONMENT = 'dev';
    					env.URL_SHORT_ENV = 'dev';
    				}
    				else if (env.NODE_LABELS.toUpperCase().contains("TEST")) {
    					env.ENVIRONMENT = 'test';
    					env.URL_SHORT_ENV = 'tst';
    				}
    				else if (env.NODE_LABELS.toUpperCase().contains("PROD")) {
    					env.ENVIRONMENT = 'prod';
    					env.URL_SHORT_ENV = 'prd';
    				}
    				
    				if (env.ENVIRONMENT == 'prod') {
    					error("Running this job in a PROD environment is not allowed!");
    				}
    				
    				// Define update sites based on environment
    				env.UPDATE_SITE = env.UPDATE_SITE_TEMPLATE.replace("%URL_SHORT_ENV%", env.URL_SHORT_ENV);
    				env.MASTER_UPDATE_SITE_URL = env.MASTER_UPDATE_SITE_URL_TEMPLATE.replace("%URL_SHORT_ENV%", env.URL_SHORT_ENV);
    			}
    		}
    	}
	    stage('Git Checkout') {
	        steps {
	        	// Get the branch that triggered this build 
	        	checkout scm
	        	
	        	// In addition get scripts required for testing and deployment
	        	// They will be checked out into a scripts directory, which resides within the root directory of the main checkout,
	        	// which is sub-optimal, but no other solution was found so far
				dir("scripts") {
					checkout([$class: 'GitSCM', branches: [[name: "${GIT_BRANCH_SCRIPTS}"]], doGenerateSubmoduleConfigurations: false, \
          			extensions: [], gitTool: 'default-git', submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket-ee-jenkins-active, \
          			url: "${GIT_REPO_SCRIPTS}"]]])
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
	            // Compiles the plugin and builds an update site from it (uses update site defined in pom.xml)
				sh(label: "Compile and Build", script: "JAVA_HOME=/apps/knime/buildtools/${JAVA_VERSION} && mvn -U clean verify -Dknime.version=${KNIME_VERSION} -Dqualifier.prefix=${QUALIFIER_PREFIX} -s ${MAVEN_SETTINGS_FILE}")
		    }    
        }
        stage('OWASP Dependency Check') {
        	when { expression { return fileExists ("${DC_HOME}/bin/dependency-check.sh") } }
        	steps {
				// Run OWASP Dependency Check CLI tool on created update site
				sh '''#!/bin/bash
					cd "${WORKSPACE}/org.rdkit.knime.update/target"
					mkdir -p reports
					${DC_HOME}/bin/dependency-check.sh --scan "./repository" --out "./reports" --format "HTML" --format "JSON" --prettyPrint --project "RDKit Nodes" --failOnCVSS ${CVSS_SCORE_THRESHOLD} --suppression "${DC_SUPPRESSION_FILE}" --proxyserver "nibr-proxy.global.nibr.novartis.net" --proxyport 2011 --nonProxyHosts "localhost,novartis.net,novartis.intra,login.microsoftonline.com"
				'''
			}                                 
            post {
				// Archive always the available OWASP dependency check results
                always {
					publishHTML reportName: 'OWASP Dependency Check Results',
							reportDir: 'org.rdkit.knime.update/target/reports',
							reportFiles: 'dependency-check-report.html',
							allowMissing: true,
							alwaysLinkToLastBuild: true,
							keepAll: false
				}
			}
		}
        stage('Run Tests') {
        	steps {
				// Installs with the Director tool a new minimal KNIME instance with the build artifacts (needs to run as bash script!)
				// The logic comes from the KNIME Community implementation (community.inc), which was slightly adapted to disable
				// accessing the KNIME Community server for test workflows, because we provide those as part of the RDKit testing feature
				// and cannot access the server from Novartis (proxy settings and credentials would be required for authentication)
				sh '''#!/bin/bash
					cd "${WORKSPACE}"
					
					# Create a link to director directly beside the community.inc script to simulate how it works on the KNIME community server
					pushd "./scripts/knime-community/"
					rm -f director
					ln -s "${DIRECTOR_HOME}" "director"
					popd
					
					# Bring in functionality of KNIME community server
					source "${WORKSPACE}/scripts/knime-community/community.inc"
					
					# This method gets called from the runTests() method after KNIME was installed, just before running tests
					configureKnimeTestInstance() {
    					local knimeFolder=$1
    					# Not used for RDKit tests
					}
					
					export LC_NUMERIC=en_US.UTF-8
					export RELEASE_REPOS="${UPDATE_SITE}"
					runTests file://${WORKSPACE}/org.rdkit.knime.update/target/repository "noServerAccess" "${WORKSPACE}/org.rdkit.knime.testing/regression-tests/zips" "" "${TEST_DEPTH_PARAMS}"
				'''
        	}
            post {
				// Archive always the available test results
                always {
                    junit 'results/**/*.xml'
                    					
					sh '''#!/bin/bash     
						# Check for hs_err_XXX file, which would tell us that the test run crashed (e.g. because of RDKit) - in that case no error would be recorded
						# and we would treat a test as successful, although it is not - Exit the script in that case
						for crashFile in "${WORKSPACE}/tmp"/hs_err_pid*.log
						do
							[ -f "${crashFile}" ] || continue
							echo "At least one test crashed - log file found: ${crashFile}"
							echo "Aborting build and testing process."
	  						exit 1
						done
					'''                
                }
            }
        } 
        stage('Deploy to Main Update Site') {
         	when {
				expression {
					// Never run deployments on PROD, only on DEV or TEST
					((env.ENVIRONMENT == 'dev' || env.ENVIRONMENT == 'test') &&
					// Never deploy master (which is the community master) to the main update site
					 ((env.DEPLOY_BRANCH_BUILDS_TO_MASTER == 'true' && !(env.git_branch_lowercase == 'master' || env.GIT_BRANCH == 'master')) || 
					  env.git_branch_lowercase == 'master_nibr' || env.GIT_BRANCH == 'master_nibr') &&
					// Never deploy any change with UNSTABLE tests to the master update site
					 (currentBuild.result == null || currentBuild.result == 'SUCCESS'))
              	}
            }        
			steps {
		        script {
					// Add successfully tested Novartis artifacts to existing Novartis update site
					sh '''#!/bin/bash
						/bin/bash "${WORKSPACE}/scripts/mirrorSingleUpdateSite.sh" "${WORKSPACE}/tmp/knime test/knime" "${DEPLOY_MASTER_UPDATE_SITE}" true true "${WORKSPACE}/scripts/mirror.xml" "${WORKSPACE}/org.rdkit.knime.update/target/repository/" true
					'''
		        }
		    }
        }
        stage('Deploy to Branch Update Site') {
         	when {
				expression {
					// Never run deployments on PROD, only on DEV or TEST
					((env.ENVIRONMENT == 'dev' || env.ENVIRONMENT == 'test') &&
					 (env.git_branch_lowercase != 'master_nibr' && env.GIT_BRANCH != 'master_nibr')) 
              	}
            }
			steps {
		        script {
					// Deploy resulting build artifacts of branches as review update with the branch name only (overriding an existing one)
					// Also UNSTABLE tests are being deployed
					sh '''#!/bin/bash 
						rm -rf "${DEPLOY_BRANCH_UPDATE_SITE}/${BRANCH_NAME}"
						mkdir -p "${DEPLOY_BRANCH_UPDATE_SITE}/${BRANCH_NAME}"
						cp -rf "${WORKSPACE}/org.rdkit.knime.update/target/repository"/* "${DEPLOY_BRANCH_UPDATE_SITE}/${BRANCH_NAME}/"
					'''
		        }
		    }
        }
        stage('Package and Rollout (only master_nibr)') {
         	when {
				expression {
					// Never run deployments on PROD, only on DEV or TEST
					((env.ENVIRONMENT == 'dev' || env.ENVIRONMENT == 'test') &&
					// Never trigger packaging for master (which is the community master) when building in Novartis environment
					 ((env.DEPLOY_BRANCH_BUILDS_TO_MASTER == 'true' && !(env.git_branch_lowercase == 'master' || env.GIT_BRANCH == 'master')) || 
					  env.git_branch_lowercase == 'master_nibr' || env.GIT_BRANCH == 'master_nibr') &&
					// Never trigger packaging for any UNSTABLE tests
					 (currentBuild.result == null || currentBuild.result == 'SUCCESS'))
              	}
            }        
			steps {
				build job: "/KNIME/knime${env.KNIME_VERSION}-${env.ENVIRONMENT}-package-linux", 
					  parameters: [string(name: "ENVIRONMENT", value: "${env.ENVIRONMENT}"), 
								 string(name: "MIRROR_UPDATE_SITE_URL", value: "${env.UPDATE_SITE}"), 
								 string(name: "NIBR_UPDATE_SITE_URL", value: "${env.MASTER_UPDATE_SITE_URL}")],
					  quietPeriod: 0, 
					  wait: false
			}
        }		 
    }
	post {
		// Use only ${env.NODE_NAME}, ${env.BUILD_URL}, ${env.JOB_NAME} and ${env.BUILD_NUMBER} in parameters
        always {
            emailext body: "The job ran on ${env.NODE_NAME}. Check console output at ${env.BUILD_URL} to view the results.",
                    recipientProviders: [developers(), requestor(), culprits()],
                    to: "${EMAIL_TO}", 
                    subject: "Jenkins job finished: ${env.JOB_NAME} - #${env.BUILD_NUMBER}"
        }
        failure {
            emailext body: "The job ran on ${env.NODE_NAME}. Check console output at ${env.BUILD_URL} to view the results.", 
                    recipientProviders: [developers(), requestor(), culprits()],
                    to: "${EMAIL_TO}", 
                    subject: "Jenkins job failed: ${env.JOB_NAME} - #${env.BUILD_NUMBER}"
        }
        unstable {
            emailext body: "The job ran on ${env.NODE_NAME}. Check console output at ${env.BUILD_URL} to view the results.",  
                    recipientProviders: [developers(), requestor(), culprits()],
                    to: "${EMAIL_TO}", 
                    subject: "Jenkins job is unstable: ${env.JOB_NAME} - #${env.BUILD_NUMBER}"
        }
        fixed {
            emailext body: "The job ran on ${env.NODE_NAME}. Check console output at ${env.BUILD_URL} to view the results.",  
                    recipientProviders: [developers(), requestor(), culprits()],
                    to: "${EMAIL_TO}", 
                    subject: "Jenkins job is fixed: ${env.JOB_NAME} - #${env.BUILD_NUMBER}"
        }
    }   
}
