def arches = [
	'arm64',
	'armel',
	'armhf',
	'ppc64le',
	's390x',
]

def dpkgArches = [
	'ppc64le': 'ppc64el',
]

// TODO worth polling any of this info off the mirrors directly?
def suites = [
	'sid': [
		'alias': 'unstable',
	],
	'stretch': [
		'alias': 'testing',
	],
	'jessie': [
		'alias': 'stable',
	],
	'wheezy': [
		'alias': 'oldstable',
		'unsupported': [
			'arm64',
			'ppc64le',
		],
	],
]

for (arch in arches) {
	dpkgArch = dpkgArches.containsKey(arch) ? dpkgArches[arch] : arch

	archSuites = []
	for (suite in suites) {
		if (suite.value.containsKey('unsupported') && suite.value['unsupported'].contains(arch)) {
			continue
		}
		archSuites << suite.key
		if (suite.value.containsKey('alias')) {
			archSuites << suite.value.alias
		}
	}

	matrixJob("docker-${arch}-debian") {
		description("""<a href="https://hub.docker.com/r/${arch}/debian/" target="_blank">Docker Hub page (<code>${arch}/debian</code>)</a>""")
		logRotator { daysToKeep(30) }
		scm {
			git {
				remote {
					url('https://github.com/tianon/docker-brew-debian.git')
					name('origin')
					refspec('+refs/heads/master:refs/remotes/origin/master')
				}
				branches('*/master')
			}
		}
		triggers {
			cron('H H * * H')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		axes {
			labelExpression('build-host', "docker-${arch}")
			text('suite', archSuites)
		}
		runSequentially() // we can only "docker push" one at a time :(
		steps {
			shell("""\
prefix='${arch}'
dpkgArch='${dpkgArch}'

sudo rm -rf */rootfs/
git clean -dfx

echo "\$dpkgArch" > arch
echo "\$prefix/debian" > repo
ln -sf ~/docker/docker/contrib/mkimage.sh

maxTries=3
while ! ./update.sh "\$suite"; do
	echo "Update failed; remaining tries: \$(( maxTries - 1 ))"
	if ! (( --maxTries )); then
		(( exitCode++ )) || true
		echo "Update failed; no tries remain; giving up and moving on"
		exit 1
	fi
	sleep 1
done

# we don't have /u/arm64
if [ "\$prefix" != 'arm64' ]; then
	docker push "\$(< repo):\$suite"
	if [ "\$(< latest)" = "\$suite" ]; then
		docker push "\$(< repo):latest"
	fi
fi
""")
		}
	}
}
