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
		shell('''\
#!/bin/bash
set -e

_all() {
	local nextPage='https://hub.docker.com/v2/repositories/library/?page_size=100'
	while true; do
		local page="$(curl -fsSL "$nextPage")"

		[ "$(echo "$page" | jq --raw-output '.results | length')" -gt 0 ] || break
		echo "$page" | jq --raw-output '.results[].name'

		nextPage="$(echo "$page" | jq --raw-output '.next')"
		[ "$nextPage" != 'null' ] || break
	done
}

diff -u \\
	<({
		ls -1 library

		# "scratch" is a special case, but exists on Docker Hub for older Docker to pull
		echo scratch

		# deprecated and removed repos
		echo docker-dev
		echo ubuntu-debootstrap
		echo ubuntu-upstart
	} | sort) \\
	<(_all | sort)
''')
	}
}
