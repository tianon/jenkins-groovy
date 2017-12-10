freeStyleJob('tianon-docker-master') {
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
		shell("""\
cd docker-master

./update.sh

docker build --pull .

git add . || true
git commit -m 'Run docker-master/update.sh' || true
""")
	}
	publishers {
		git {
			branch('origin', 'master')
			pushOnlyIfSuccess()
		}
	}
}
