freeStyleJob('official-images-docs-tag-details') {
	logRotator { numToKeep(5) }
	label('infosiftr')
	scm {
		git {
			remote {
				url('git@github.com:docker-library/repo-info.git')
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
		cron('H * * * *')
	}
	wrappers { colorizeOutput() }
	steps {
		shell('''\
# get "bashbrew" from master
curl -fSL 'https://github.com/docker-library/official-images/archive/master.tar.gz' \\
	| tar -xz
mkdir official-images-master/bin
ln -s ../bashbrew/bashbrew.sh official-images-master/bin/bashbrew
export PATH="$PWD/official-images-master/bin:$PATH"
export BASHBREW_LIBRARY="$PWD/official-images-master/library"

./update-tag-details.sh
git add repos/*/tag-details.md || true
git commit -m 'Run update-tag-details.sh' || true

# try catching up since this job takes so long to run
git checkout -- .
git clean -dfx .
git pull --rebase https://github.com/docker-library/repo-info.git master || true
''')
	}
	publishers {
		git {
			branch('origin', 'master')
			pushOnlyIfSuccess()
		}
	}
}
