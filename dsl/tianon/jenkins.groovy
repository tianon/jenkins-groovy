freeStyleJob('tianon-jenkins') {
	logRotator { numToKeep(5) }
	label('tianon-nameless')
	scm {
		git {
			remote {
				url('git@github.com:tianon/dockerfiles.git')
				credentials('tianon')
			}
			branches('*/master')
			clean()
		}
	}
	triggers {
		cron('H H * * *')
		scm('H/5 * * * *')
	}
	wrappers { colorizeOutput() }
	steps {
		shell("""\
cd jenkins

./update.sh

docker build --pull -t tianon/jenkins .

jenkinsVersion="\$(awk '
	\$1 == "ENV" && \$2 == "JENKINS_VERSION" {
		print \$3;
	}
' Dockerfile)"

git commit -m "Update jenkins to \$jenkinsVersion" -- Dockerfile || true

docker tag -f tianon/jenkins "tianon/jenkins:\$jenkinsVersion"
docker push "tianon/jenkins:\$jenkinsVersion"
""")
	}
	publishers {
		git {
			branch('origin', 'master')
			pushOnlyIfSuccess()
		}
	}
}
