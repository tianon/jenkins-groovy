def arches = [
	'arm64',
	//'armel',
	//'armhf',
	//'ppc64le',
	//'s390x',
]

def dpkgArches = [
	'ppc64le': 'ppc64el',
]

// TODO poll these off https://github.com/tianon/docker-brew-debian ?
def suites = [
	'jessie',
	'oldstable',
	'sid',
	'squeeze',
	'stable',
	'stretch',
	'testing',
	'unstable',
	'wheezy',
]

// TODO poll these off the mirror directly?
def unsupportedSuites = [
	'armhf': [ 'squeeze' ],
	'armel': [],
	'arm64': [],
	'ppc64le': [],
	's390x': [],
]

for (arch in arches) {
	dpkgArch = dpkgArches.containsKey(arch) ? dpkgArches[arch] : arch

	archSuites = []
	for (suite in suites) {
		if (!unsupportedSuites[arch].contains(suite)) {
			archSuites << suite
		}
	}

	matrixJob("docker-${arch}-debian") {
		logRotator { daysToKeep(30) }
		label("docker-${arch}")
		scm {
			git {
				remote {
					github('tianon/docker-brew-debian')
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
			text('SUITE', archSuites)
		}
		steps {
			shell("""\
sudo rm -rf */rootfs/
git clean -dfx

echo '${dpkgArch}' > arch
echo '${arch}/debian' > repo
ln -sf ~/docker/docker/contrib/mkimage.sh

maxTries=3
while ! ./update.sh "\$SUITE"; do
	echo "Update failed; remaining tries: \$(( maxTries - 1 ))"
	if ! (( --maxTries )); then
		(( exitCode++ )) || true
		echo "Update failed; no tries remain; giving up and moving on"
		exit 1
	fi
	sleep 1
done

docker push "\$(< repo):\$SUITE"
if [ "\$(< latest)" = "\$SUITE" ]; then
	docker push "\$(< repo):latest"
fi
""")
		}
	}
}
