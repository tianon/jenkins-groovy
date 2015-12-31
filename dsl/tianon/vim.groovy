freeStyleJob('tianon-vim-docker') {
	logRotator { numToKeep(5) }
	label('tianon')
	scm {
		git {
			remote {
				url('https://github.com/docker/docker.git')
				name('docker')
				refspec('+refs/heads/master:refs/remotes/docker/master')
			}
			remote {
				url('git@github.com:tianon/vim-docker.git')
				credentials('tianon')
				name('tianon')
				refspec('+refs/heads/master:refs/remotes/tianon/master')
			}
			branches('docker/master')
			clean()
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
