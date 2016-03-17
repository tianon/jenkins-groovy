import vars.multiarch

for (arch in multiarch.allArches([
	// no binaries available
	'arm64',
	's390x',
	// see https://bitbucket.org/pypy/pypy/downloads for upstream binary downloads

	// "pypy: error while loading shared libraries: libssl.so.10: cannot open shared object file: No such file or directory"
	'ppc64le',
])) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
		scm {
			git {
				remote { url('https://github.com/docker-library/pypy.git') }
				branches('*/master')
				extensions {
					cleanAfterCheckout()
				}
			}
		}
		triggers {
			upstream("docker-${arch}-debian, docker-${arch}-buildpack", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta, ['dpkgArch']) + '''
case "$dpkgArch" in
	armel)
		sed -i 's!linux64!linux-armel!g' */{,*/}Dockerfile
		;;
	armhf)
		sed -i 's!linux64!linux-armhf-raring!g' */{,*/}Dockerfile
		;;
	i386)
		sed -i 's!linux64!linux!g' */{,*/}Dockerfile
		;;
	ppc64el)
		sed -i 's!linux64!ppc64le!g' */{,*/}Dockerfile
		# now for the bizarre...  4.0.0 on bitbucket had ppc64le, but not 4.0.1
		sed -i 's!https://bitbucket.org/pypy/pypy/downloads/!http://cobra.cs.uni-duesseldorf.de/~buildmaster/mirror/!g' */{,*/}Dockerfile
		# no pypy3 love for ppc64le (yet?)
		rm -r 3
		;;
	*)
		echo >&2 "unsupported architecture: $prefix"
		exit 1
		;;
esac

sed -i "s!^FROM !FROM $prefix/!" */{,*/}Dockerfile

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"

for v in */; do
	v="${v%/}"
	docker build -t "$repo:$v" "$v"
	docker build -t "$repo:$v-onbuild" "$v/onbuild"
	docker build -t "$repo:$v-slim" "$v/slim"
	if [ "$v" = "$latest" ]; then
		docker tag -f "$repo:$v" "$repo"
		docker tag -f "$repo:$v-onbuild" "$repo:onbuild"
		docker tag -f "$repo:$v-slim" "$repo:slim"
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
