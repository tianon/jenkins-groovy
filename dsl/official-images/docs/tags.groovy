freeStyleJob('official-images-docs-tag-details') {
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
			clean()
			configure { node ->
				node / 'extensions' / 'hudson.plugins.git.extensions.impl.UserIdentity' {
					delegate.name('Docker Library Bot')
					delegate.email('github+dockerlibrarybot@infosiftr.com')
				}
			}
		}
	}
	triggers {
		cron('H * * * *')
	}
	wrappers { colorizeOutput() }
	steps {
		shell("""\
./update-tag-details.sh
git add */tag-details.md || true
git commit -m 'Run update-tag-details.sh' || true
""")
	}
	publishers {
		git {
			branch('origin', 'master')
			pushOnlyIfSuccess()
		}
	}
}
