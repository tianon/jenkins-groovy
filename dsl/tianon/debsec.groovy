freeStyleJob('tianon-debian-security-mirror') {
	logRotator { daysToKeep(30) }
	concurrentBuild(false)
	label('tianon-zoe')
	scm {
		git {
			remote {
				url('git@github.com:tianon/debian-security-tracker-mirror.git')
				credentials('tianon')
			}
			branches('*/master')
			extensions {
				cleanAfterCheckout()
			}
		}
	}
	triggers {
		cron('H/30 * * * *')
	}
	wrappers { colorizeOutput() }
	steps {
		shell("""\
./update.sh

git add debian-security*.json || true
git commit -m 'Update debian-security.json' || true
""")
	}
	publishers {
		git {
			branch('origin', 'master')
			pushOnlyIfSuccess()
		}
	}
}
