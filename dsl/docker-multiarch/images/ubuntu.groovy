def arches = [
	'arm64',
	//'armhf',
	//'ppc64le',
]

def dpkgArches = [
	'ppc64le': 'ppc64el',
]

for (arch in arches) {
	dpkgArch = dpkgArches.containsKey(arch) ? dpkgArches[arch] : arch

	freeStyleJob("docker-${arch}-ubuntu") {
		logRotator { daysToKeep(30) }
		label("docker-${arch}")
		scm {
			git {
				remote {
					github('tianon/docker-brew-ubuntu-core')
					name('origin')
					refspec('+refs/heads/master:refs/remotes/origin/master')
				}
				branches('*/master')
			}
		}
		triggers {
			cron('H H * * *')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell("""\
echo '${dpkgArch}' > arch
echo '${arch}/ubuntu' > repo
./update.sh

# we don't have /u/arm64
if [ '${arch}' != 'arm64' ]; then
	docker images "\$(< repo)" \
		| awk -F '  +' 'NR>1 { print \$1 ":" \$2 }' \
		| xargs -rtn1 docker push
fi
""")
		}
	}
}
