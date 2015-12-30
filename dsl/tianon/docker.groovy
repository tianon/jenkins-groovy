freeStyleJob('tianon-docker-master') {
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
cd docker

./update.sh

./build.sh

git commit -m 'Run docker/update.sh' -- . || true

./push.sh
""")
	}
	publishers {
		git {
			branch('origin', 'master')
			pushOnlyIfSuccess()
		}
	}
}
