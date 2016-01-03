freeStyleJob('tianon-debian-security-mirror') {
	logRotator { daysToKeep(30) }
	label('tianon')
	scm {
		git {
			remote {
				url('git@github.com:tianon/debian-security-tracker-mirror.git')
				credentials('tianon')
			}
			branches('*/master')
			clean()
		}
	}
	triggers {
		cron('H * * * *')
	}
	wrappers { colorizeOutput() }
	steps {
		shell("""\
./update.sh

git commit -m 'Update debian-security.json' -- debian-security.json || true
""")
	}
	publishers {
		git {
			branch('origin', 'master')
			pushOnlyIfSuccess()
		}
	}
}
