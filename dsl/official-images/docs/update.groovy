freeStyleJob('official-images-docs-update') {
	disabled()
	logRotator { numToKeep(5) }
	label('infosiftr')
	scm {
		git {
			remote {
				url('git@github.com:docker-library/docs.git')
				credentials('docker-library-bot')
				name('')
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
		cron('H/15 * * * *')
	}
	wrappers { colorizeOutput() }
	steps {
		shell("""\
./update.sh
git commit -m 'Run update.sh' -- \\
		'*/README.md' \\
		hello-world/content.md \\
	|| true
""")
	}
	publishers {
		git {
			branch('origin', 'master')
			pushOnlyIfSuccess()
		}
	}
}
