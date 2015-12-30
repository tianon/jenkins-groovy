def arches = [
	//'arm64',
	'armel',
	'armhf',
	//'ppc64le', // "pypy: error while loading shared libraries: libssl.so.10: cannot open shared object file: No such file or directory"
	//'s390x',
]
// see https://bitbucket.org/pypy/pypy/downloads for upstream binary downloads

for (arch in arches) {
	freeStyleJob("docker-${arch}-pypy") {
		description("""<a href="https://hub.docker.com/r/${arch}/pypy/" target="_blank">Docker Hub page (<code>${arch}/pypy</code>)</a>""")
		logRotator { daysToKeep(30) }
		label("docker-${arch}")
		scm {
			git {
				remote { url('https://github.com/docker-library/pypy.git') }
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
repo="\$prefix/pypy"

case "\$prefix" in
	armel)
		sed -i 's!linux64!linux-armel!g' */{,*/}Dockerfile
		;;
	armhf)
		sed -i 's!linux64!linux-armhf-raring!g' */{,*/}Dockerfile
		;;
	ppc64le)
		sed -i 's!linux64!ppc64le!g' */{,*/}Dockerfile
		# now for the bizarre...  4.0.0 on bitbucket had ppc64le, but not 4.0.1
		sed -i 's!https://bitbucket.org/pypy/pypy/downloads/!http://cobra.cs.uni-duesseldorf.de/~buildmaster/mirror/!g' */{,*/}Dockerfile
		# no pypy3 love for ppc64le (yet?)
		rm -r 3
		;;
	*)
		echo >&2 "unsupported architecture: \$prefix"
		exit 1
		;;
esac

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
