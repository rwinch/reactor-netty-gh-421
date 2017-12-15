def projectProperties = [
	[$class: 'BuildDiscarderProperty',
		strategy: [$class: 'LogRotator', numToKeepStr: '50']])
]
properties(projectProperties)

stage('Check Dependencies') {
	node {
		try {
			checkout scm
			sh "./gradlew clean check --stacktrace --no-daemon --refresh-dependencies"
		} finally {
			junit '**/build/*-results/test/*.xml'
		}
	}
}
