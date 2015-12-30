def arches = [
	'arm64',
	//'armel', // unsupported; upstream considers the arch effectively dead
	'armhf',
	'ppc64le',
	//'s390x', // unsupported; upstream doesn't consider the arch important
]

def dpkgArches = [
	'ppc64le': 'ppc64el',
]

for (arch in arches) {
	dpkgArch = dpkgArches.containsKey(arch) ? dpkgArches[arch] : arch

	freeStyleJob("docker-${arch}-ubuntu") {
		description("""<a href="https://hub.docker.com/r/${arch}/ubuntu/" target="_blank">Docker Hub page (<code>${arch}/ubuntu</code>)</a>""")
		logRotator { daysToKeep(30) }
		label("docker-${arch}")
		scm {
			git {
				remote {
					url('https://github.com/tianon/docker-brew-ubuntu-core.git')
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
prefix='${arch}'
dpkgArch='${dpkgArch}'

echo "\$dpkgArch" > arch
echo "\$prefix/ubuntu" > repo
./update.sh

# we don't have /u/arm64
if [ "\$prefix" != 'arm64' ]; then
	docker images "\$(< repo)" \\
		| awk -F '  +' 'NR>1 { print \$1 ":" \$2 }' \\
		| xargs -rtn1 docker push
fi
""")
		}
	}
}
