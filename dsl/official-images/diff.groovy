freeStyleJob('official-images-diff') {
	logRotator { daysToKeep(30) }
	label('infosiftr')
	scm {
		git {
			remote { url('https://github.com/docker-library/official-images.git') }
			branches('*/master')
			extensions {
				cleanAfterCheckout()
			}
		}
	}
	triggers {
		cron('H H * * *')
		scm('H/5 * * * *')
	}
	wrappers { colorizeOutput() }
	steps {
		shell("""\
diff -u \\
	<({ echo scratch; ls -1 library; } \\
		| sort) \\
	<(curl -fsSL 'https://hub.docker.com/v2/repositories/library/?page_size=1000&page=1' \\
		| jq --raw-output '.results[].name' \\
		| sort)
""")
	}
}
