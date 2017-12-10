freeStyleJob('tianon-jenkins') {
	logRotator { numToKeep(5) }
	concurrentBuild(false)
	label('tianon-zoe')
	scm {
		git {
			remote {
				url('git@github.com:tianon/dockerfiles.git')
				credentials('tianon')
			}
			branches('*/master')
			extensions {
				cleanAfterCheckout()
			}
		}
	}
	triggers {
		cron('H H * * *')
		scm('H/5 * * * *')
	}
	wrappers { colorizeOutput() }
	steps {
		shell('''\
cd jenkins

./update.sh

docker build --pull -t tianon/jenkins .

jenkinsVersion="$(awk '
	$1 == "ENV" && $2 == "JENKINS_VERSION" {
		print $3
	}
' Dockerfile)"

git commit -m "Update jenkins to $jenkinsVersion" -- Dockerfile || true

docker tag tianon/jenkins "tianon/jenkins:$jenkinsVersion"
docker push "tianon/jenkins:$jenkinsVersion"
docker push tianon/jenkins:latest
''')
	}
	publishers {
		git {
			branch('origin', 'master')
			pushOnlyIfSuccess()
		}
	}
}
