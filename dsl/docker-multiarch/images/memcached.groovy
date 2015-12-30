def arches = [
	'arm64',
	'armel',
	'armhf',
	'ppc64le',
	's390x',
]

for (arch in arches) {
	freeStyleJob("docker-${arch}-memcached") {
		description("""<a href="https://hub.docker.com/r/${arch}/memcached/" target="_blank">Docker Hub page (<code>${arch}/memcached</code>)</a>""")
		logRotator { daysToKeep(30) }
		label("docker-${arch}")
		scm {
			git {
				remote { url('https://github.com/docker-library/memcached.git') }
				branches('*/master')
				clean()
			}
		}
		triggers {
			upstream("docker-${arch}-debian", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell("""\
prefix='${arch}'
repo="\$prefix/memcached"

sed -i "s!^FROM !FROM \$prefix/!" Dockerfile

docker build -t "\$repo" .
version="\$(awk '\$1 == "ENV" && \$2 == "MEMCACHED_VERSION" { print \$3; exit }' Dockerfile)"
docker tag -f "\$repo" "\$repo:\$version"

# we don't have /u/arm64
if [ "\$prefix" != 'arm64' ]; then
	docker images "\$repo" \\
		| awk -F '  +' 'NR>1 { print \$1 ":" \$2 }' \\
		| xargs -rtn1 docker push
fi
""")
		}
	}
}
