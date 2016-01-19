freeStyleJob('official-images-labeler') {
	logRotator { numToKeep(5) }
	label('infosiftr')
	scm {
		git {
			remote { url('https://github.com/yosifkit/official-images-issue-labeler.git') }
			branches('*/master')
			clean()
		}
	}
	triggers {
		cron('H/15 * * * *')
		//scm('H/5 * * * *')
	}
	wrappers {
		colorizeOutput()
		credentialsBinding {
			string('GITHUB_TOKEN', 'github-token-docker-library-bot')
		}
	}
	steps {
		shell('''\
docker build -t docker-library-issue-labeler .
set +x
docker run --rm docker-library-issue-labeler \\
	--token "$GITHUB_TOKEN" \\
	--owner docker-library \\
	--repo official-images \\
	--state open
''')
	}
}
