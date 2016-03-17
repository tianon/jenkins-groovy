freeStyleJob('official-images-docs-update') {
	logRotator { numToKeep(5) }
	label('infosiftr')
	scm {
		git {
			remote {
				url('git@github.com:docker-library/docs.git')
				credentials('docker-library-bot')
				name('origin')
			}
			branches('*/master')
			extensions {
				cleanAfterCheckout()
			}
			configure { node ->
				node / 'extensions' / 'hudson.plugins.git.extensions.impl.UserIdentity' {
					delegate.name('Docker Library Bot')
					delegate.email('github+dockerlibrarybot@infosiftr.com')
				}
			}
		}
	}
	triggers {
		cron('H/15 * * * *')
	}
	wrappers { colorizeOutput() }
	steps {
		shell("""\
./update.sh
git add */README.md || true
git add hello-world/content.md || true
git commit -m 'Run update.sh' || true
""")
	}
	publishers {
		git {
			branch('origin', 'master')
			pushOnlyIfSuccess()
		}
	}
}
