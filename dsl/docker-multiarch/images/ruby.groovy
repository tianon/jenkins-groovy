def arches = [
	'arm64',
	'armel',
	'armhf',
	'ppc64le',
	's390x',
]

for (arch in arches) {
	freeStyleJob("docker-${arch}-ruby") {
		description("""<a href="https://hub.docker.com/r/${arch}/ruby/" target="_blank">Docker Hub page (<code>${arch}/ruby</code>)</a>""")
		logRotator { daysToKeep(30) }
		label("docker-${arch}")
		scm {
			git {
				remote { url('https://github.com/docker-library/ruby.git') }
				branches('*/master')
				clean()
			}
		}
		triggers {
			upstream("docker-${arch}-debian, docker-${arch}-buildpack", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell("""\
prefix='${arch}'
repo="\$prefix/ruby"

# Ruby 2.0 sucks and needs to die (also, config.guess is outdated and Ruby 2.0 isn't worth hacking it to a newer version for IMO)
rm -r 2.0

sed -i "s!^FROM !FROM \$prefix/!" */{,*/}Dockerfile

latest="\$(./generate-stackbrew-library.sh | awk '\$1 == "latest:" { print \$3; exit }')"

for v in */; do
	v="\${v%/}"
	docker build -t "\$repo:\$v" "\$v"
	docker build -t "\$repo:\$v-onbuild" "\$v/onbuild"
	docker build -t "\$repo:\$v-slim" "\$v/slim"
	if [ "\$v" = "\$latest" ]; then
		docker tag -f "\$repo:\$v" "\$repo"
		docker tag -f "\$repo:\$v-onbuild" "\$repo:onbuild"
		docker tag -f "\$repo:\$v-slim" "\$repo:slim"
	fi
done

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
