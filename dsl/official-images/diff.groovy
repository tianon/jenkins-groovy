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
	<({ echo scratch; ls -1 library; } | sort) \\
	<(_all | sort)
""")
	}
}
