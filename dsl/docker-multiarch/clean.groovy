def multiarch = (new GroovyShell()).evaluate(readFileFromWorkspace('vars/multiarch.groovy'))

matrixJob('docker-multiarch-clean') {
	logRotator { daysToKeep(30) }
	concurrentBuild(false)
	label('master')
	triggers {
		cron('H H/12 * * *')
	}
	axes {
		label('host', multiarch.allNodes())
	}
	wrappers { colorizeOutput() }
	steps {
		shell('''\
docker version

docker ps -aq \\
	| xargs --no-run-if-empty --verbose docker rm || true

docker images -q --filter 'dangling=true' \\
	| xargs --no-run-if-empty --verbose docker rmi || true
''')
	}
}
