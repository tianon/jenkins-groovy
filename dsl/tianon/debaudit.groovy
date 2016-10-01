// TODO scrape these automatically from the two relevant repo's folders
def images = [
	'debian': [
		'unstable',
		'testing',
		'stable',
		'oldstable',
	],
	'ubuntu': [
		'precise',
		'trusty',
		'xenial',
		'yakkety',
	],
]

def firehose = [
	'debian:unstable', 'debian:sid',
	'debian:testing', 'debian:stretch',
	'ubuntu:devel', 'ubuntu:yakkety',
]

images.each { repo, suites ->
	matrixJob('tianon-audit-' + repo) {
		logRotator { daysToKeep(30) }
		label('tianon')
		triggers {
			cron('H H/12 * * *')
		}
		axes {
			labelExpression('build-host', 'tianon')
			text('suite', suites)
		}
		wrappers { colorizeOutput() }
		steps {
			shell("""\
#!/bin/bash
set -eo pipefail

image="${repo}:\$suite"

docker pull "\$image" > /dev/null

echo; echo

docker run -i --rm "\$image" sh -ec '
	apt-get update -qq
	apt-get dist-upgrade -qq -s
' | awk -F '[ ()\\\\[\\\\]]+' '
	\$1 == "Inst" {
		print \$2 " -- " \$3 " => " \$4
	}
' | sort | tee temp

echo; echo

for s in ${firehose.join(' ')}; do
	if [ "\$image" = "\$s" ]; then
		# if the image we're testing is a "firehose" image,
		# then don't mark updates available as a failing build
		exit
	fi
done

[ ! -s temp ]
""")
		}
	}
}
