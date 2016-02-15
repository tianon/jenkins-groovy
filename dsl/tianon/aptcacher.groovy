matrixJob('tianon-apt-cacher-maint') {
	logRotator { numToKeep(5) }
	label('tianon')
	triggers {
		cron('H H * * *')
	}
	axes {
		label('host', 'tianon-nameless', 'tianon-viper')
	}
	wrappers { colorizeOutput() }
	steps {
		shell('''\
if docker inspect --type container apt-cacher-ng > /dev/null; then
	docker exec apt-cacher-ng /etc/cron.daily/apt-cacher-ng
fi
''')
	}
}
