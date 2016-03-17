freeStyleJob('official-images-docs-push') {
	logRotator { numToKeep(5) }
	label('infosiftr')
	scm {
		git {
			remote { url('https://github.com/docker-library/docs.git') }
			branches('*/master')
			extensions {
				cleanAfterCheckout()
			}
		}
	}
	triggers {
		upstream('official-images-docs-update', 'UNSTABLE')
	}
	wrappers {
		colorizeOutput()
		credentialsBinding {
			usernamePassword('USERNAME', 'PASSWORD', 'docker-hub-stackbrew')
		}
	}
	steps {
		shell("""\
docker build --pull -t docker-library-docs .
test -t 1 && it='-it' || it='-i'
set +x
docker run "\$it" --rm -e TERM \\
	--entrypoint './push.pl' \\
	docker-library-docs \\
	--username "\$USERNAME" \\
	--password "\$PASSWORD" \\
	--batchmode */
""")
	}
}
