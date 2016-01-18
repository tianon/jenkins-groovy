import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
		scm {
			git {
				remote { url('https://github.com/docker-library/gcc.git') }
				branches('*/master')
				clean()
			}
		}
		triggers {
			upstream("docker-${arch}-buildpack", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta, ['gnuArch']) + '''
sed -i "s!^FROM !FROM $prefix/!" */Dockerfile

# explicitly set the gcc arch tuple to the arch of gcc from the build environment
# (this makes sure our armhf build on arm64 hardware builds an armhf gcc)
sed -i "s!/configure !/configure --build='$gnuArch' !g" */Dockerfile

# update autoconf config.guess and config.sub so they support our architectures for sure
sed -i 's!.*/configure !\\t\\&\\& ( set -e; cd /usr/src/gcc; for f in config.guess config.sub; do curl -fSL "http://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=$f;hb=HEAD" -o "$f"; find -mindepth 2 -name "$f" -exec cp -v "$f" "{}" ";"; done ) \\\\\\n&!' */Dockerfile

(
	set +x
	for v in */; do
		v="${v%/}"
		from="$(awk '$1 == "FROM" { print $2; exit }' "$v/Dockerfile")"
		if ! docker inspect "$from" &> /dev/null; then
			echo >&2 "warning, '$from' does not exist; skipping $v"
			rm -r "$v/"
		fi
	done
)

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"

for v in */; do
	v="${v%/}"
	docker build -t "$repo:$v" "$v"
	if [ "$v" = "$latest" ]; then
		docker tag -f "$repo:$v" "$repo"
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
