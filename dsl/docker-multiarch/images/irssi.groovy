def arches = [
	'arm64',
	'armel',
	'armhf',
	'ppc64le',
	's390x',
]

for (arch in arches) {
	freeStyleJob("docker-${arch}-irssi") {
		description("""<a href="https://hub.docker.com/r/${arch}/irssi/" target="_blank">Docker Hub page (<code>${arch}/irssi</code>)</a>""")
		logRotator { daysToKeep(30) }
		label("docker-${arch}")
		scm {
			git {
				remote { url('https://github.com/jfrazelle/irssi.git') }
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
repo="\$prefix/irssi"

sed -i "s!^FROM !FROM \$prefix/!" Dockerfile

docker build -t "\$repo" .
version="\$(awk '\$1 == "ENV" && \$2 == "IRSSI_VERSION" { print \$3; exit }')"
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
