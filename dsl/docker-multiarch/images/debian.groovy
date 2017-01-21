import vars.multiarch

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
		'supported': [
			// thanks to LTS, no more wheezy except on these arches
			// http://security.debian.org/dists/wheezy/updates/main/
			'amd64',
			'armel',
			'armhf',
			'i386',
		],
	],
]

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)
	if (meta.dpkgArch == null) {
		// skip unsupported architectures
		continue
	}

	archSuites = []
	for (suite in suites) {
		if (suite.value.containsKey('supported') && !suite.value['supported'].contains(arch)) {
			continue
		}
		if (suite.value.containsKey('unsupported') && suite.value['unsupported'].contains(arch)) {
			continue
		}
		archSuites << suite.key
		if (suite.value.containsKey('alias')) {
			archSuites << suite.value.alias
		}
	}

	matrixJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		concurrentBuild(false)
		label(meta.label)
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
			labelExpression('build-host', meta.label)
			text('suite', archSuites)
		}
		runSequentially() // we can only "docker push" one at a time :(
		steps {
			shell(multiarch.templateArgs(meta, ['dpkgArch']) + '''
sudo rm -rf */rootfs/
git clean -dfx

echo "$dpkgArch" > arch
echo "$repo" > repo
ln -sf ~/docker/docker/contrib/mkimage.sh

maxTries=3
while ! ./update.sh "$suite"; do
	echo "Update failed; remaining tries: $(( maxTries - 1 ))"
	if ! (( --maxTries )); then
		(( exitCode++ )) || true
		echo "Update failed; no tries remain; giving up and moving on"
		exit 1
	fi
	sleep 1
done
''' + multiarch.templatePush(meta))
		}
	}
}
