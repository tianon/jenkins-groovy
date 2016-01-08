import vars.multiarch

matrixJob('docker-multiarch-clean') {
	logRotator { daysToKeep(30) }
	triggers {
		cron('H H/12 * * *')
	}
	axes {
		label('host', multiarch.allArches([
			// these run on arm64 (no dedicated worker hosts)
			'armel',
			'armhf',
		]).collect { "docker-${it}" })
	}
	wrappers { colorizeOutput() }
	steps {
		shell('''
docker ps -aq \\
	| xargs --no-run-if-empty --verbose docker rm || true

docker images -q --filter 'dangling=true' \\
	| xargs --no-run-if-empty --verbose docker rmi || true
''')
	}
}
