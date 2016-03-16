freeStyleJob('tianon-boot2docker-ga') {
	logRotator { numToKeep(5) }
	label('tianon-nameless')
	scm {
		git {
			remote {
				url('https://github.com/boot2docker/boot2docker.git')
			}
			branches('*/master')
			clean()
		}
	}
	triggers {
		//scm('H/5 * * * *')
	}
	wrappers { colorizeOutput() }
	steps {
		shell('''\
git-set-mtimes
docker build -t boot2docker/boot2docker --pull .
docker run --rm boot2docker/boot2docker > boot2docker.iso
''')
	}
	publishers {
		archiveArtifacts {
			fingerprint()
			pattern('boot2docker.iso')
		}
	}
}
