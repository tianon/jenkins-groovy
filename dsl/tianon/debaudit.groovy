// TODO scrape these automatically from the two relevant repo's folders
def images = [
	'debian': [
		'unstable',
		'testing',
		'stable',
		'oldstable',
		'squeeze',
	],
	'ubuntu': [
		'precise',
		'trusty',
		'vivid',
		'wily',
		'xenial',
	],
]

def imagesAxis = []
images.each { repo, suites ->
	suites.each { suite ->
		imagesAxis << repo + ':' + suite
	}
}

matrixJob('tianon-audit-deb-images') {
	logRotator { daysToKeep(30) }
	concurrentBuild()
	triggers {
		cron('H H/12 * * *')
	}
	axes {
		labelExpression('build-host', 'tianon')
		text('image', imagesAxis)
	}
	wrappers { colorizeOutput() }
	steps {
		shell("""\
docker run -i --rm "\$image" sh -ec '
	apt-get update -qq
	apt-get dist-upgrade -qq -s
' | -F '[ \\[\\]()]+' '\$1 == "Inst" { print \$2 " -- " \$3 " => " \$4 }'
""")
	}
}
