freeStyleJob('tianon-vim-docker') {
	logRotator { numToKeep(5) }
	concurrentBuild(false)
	label('tianon-zoe')
	scm {
		git {
			remote {
				url('https://github.com/docker/docker.git')
				name('docker')
				refspec('+refs/heads/master:refs/remotes/docker/master')
			}
			remote {
				url('git@github.com:tianon/vim-docker.git')
				credentials('ssh-tianon')
				name('tianon')
				refspec('+refs/heads/master:refs/remotes/tianon/master')
			}
			branches('docker/master')
			extensions {
				cleanAfterCheckout()
			}
		}
	}
	triggers {
		scm('H H * * *')
	}
	wrappers { colorizeOutput() }
	steps {
		shell("""\
git filter-branch \\
	--prune-empty \\
	--subdirectory-filter contrib/syntax/vim \\
	--force
""")
	}
	publishers {
		git {
			branch('tianon', 'master')
			pushOnlyIfSuccess()
			forcePush()
		}
	}
}
