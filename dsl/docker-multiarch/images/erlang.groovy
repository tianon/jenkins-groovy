import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
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
			shell(multiarch.templateArgs(meta, ['gccArch']) + '''
# ignore ancient versions
rm -r R*/ elixir/

sed -i "s!^FROM !FROM $prefix/!" */{,*/}Dockerfile

# explicitly set the gcc arch tuple to the arch of gcc from the build environment
# (this makes sure our armhf build on arm64 hardware builds an armhf gcc)
sed -i "s!/configure !/configure --build='$gccArch' !g" */Dockerfile

# update autoconf config.guess and config.sub so they support our architectures for sure
sed -i 's!.* autoconf !\\t\\&\\& ( cd erts/autoconf \\&\\& curl -fSL "http://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.guess;hb=HEAD" -o config.guess \\&\\& curl -fSL "http://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.sub;hb=HEAD" -o config.sub ) \\\\\\n&!' */{,*/}Dockerfile

latest='18' # TODO discover this automatically somehow

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
