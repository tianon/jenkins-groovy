def arches = [
	'arm64',
	'armel',
	'armhf',
	'ppc64le',
	's390x',
]

for (arch in arches) {
	freeStyleJob("docker-${arch}-erlang") {
		description("""<a href="https://hub.docker.com/r/${arch}/erlang/" target="_blank">Docker Hub page (<code>${arch}/erlang</code>)</a>""")
		logRotator { daysToKeep(30) }
		label("docker-${arch}")
		scm {
			git {
				remote { url('https://github.com/c0b/docker-erlang-otp.git') }
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
repo="\$prefix/erlang"

# ignore ancient versions
rm -r R*/ elixir/

sed -i "s!^FROM !FROM \$prefix/!" */{,*/}Dockerfile

# update autoconf config.guess and config.sub so they support our architectures for sure
sed -i 's!.* autoconf !\\t\\&\\& ( cd erts/autoconf \\&\\& curl -fSL "http://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.guess;hb=HEAD" -o config.guess \\&\\& curl -fSL "http://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.sub;hb=HEAD" -o config.sub ) \\\\\\n&!' */{,*/}Dockerfile

latest='18' # TODO discover this automatically somehow

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
