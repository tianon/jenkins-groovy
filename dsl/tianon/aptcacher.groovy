matrixJob('tianon-apt-cacher-maint') {
	logRotator { numToKeep(5) }
	label('tianon-nameless')
	triggers {
		cron('H H * * *')
	}
	axes {
		label('host',
			'tianon-nameless',
			'tianon-viper',
			'yosifkit-minas-morgul',
		)
	}
	wrappers { colorizeOutput() }
	steps {
		shell('''\
if docker inspect --type container apt-cacher-ng > /dev/null; then
	docker exec apt-cacher-ng \\
		/usr/lib/apt-cacher-ng/acngtool \\
			maint \\
				-c /etc/apt-cacher-ng \\
				SocketPath=/var/run/apt-cacher-ng/socket \\
				--verbose
fi
''')
	}
}
