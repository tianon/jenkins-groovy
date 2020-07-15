freeStyleJob('tianon-jenkins') {
	logRotator { numToKeep(5) }
	concurrentBuild(false)
	label('tianon-zoe')
	scm {
		git {
			remote {
				url('git@github.com:tianon/dockerfiles.git')
				credentials('ssh-git')
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
cd jenkins/weekly
./update.sh
docker build --pull -t tianon/jenkins:weekly .

jenkinsVersion="$(awk '
	$1 == "ENV" && $2 == "JENKINS_VERSION" {
		print $3
	}
' Dockerfile)"
git commit -m "Update jenkins-weekly to $jenkinsVersion" -- Dockerfile || true
''')
		shell('''\
cd jenkins/lts
./update.sh
docker build --pull -t tianon/jenkins:lts .

jenkinsVersion="$(awk '
	$1 == "ENV" && $2 == "JENKINS_VERSION" {
		print $3
	}
' Dockerfile)"
git commit -m "Update jenkins-lts to $jenkinsVersion" -- Dockerfile || true
''')
	}
	publishers {
		git {
			branch('origin', 'master')
			pushOnlyIfSuccess()
		}
	}
}
