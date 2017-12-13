def projectProperties = [
	[$class: 'BuildDiscarderProperty',
		strategy: [$class: 'LogRotator', numToKeepStr: '1000']],
	pipelineTriggers([cron('*/1 * * * *')])
]
properties(projectProperties)

def SUCCESS = hudson.model.Result.SUCCESS.toString()
currentBuild.result = SUCCESS

stage('Check Dependencies') {
	node {
		try {
			checkout scm
			sh "./gradlew clean check --stacktrace --no-daemon --refresh-dependencies"
		} catch(Throwable t) {
			final def RECIPIENTS = [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']]
			def subject = "FAILING: Build ${env.JOB_NAME} ${env.BUILD_NUMBER} status is now FAILING"
			def details = """The build is FAILING. For details see ${env.BUILD_URL}"""
			emailext (
				subject: subject,
				body: details,
				recipientProviders: RECIPIENTS,
				to: "$SPRING_SECURITY_TEAM_EMAILS"
			)
			throw t;
		} finally {
			junit '**/build/*-results/test/*.xml'
		}
	}
}
