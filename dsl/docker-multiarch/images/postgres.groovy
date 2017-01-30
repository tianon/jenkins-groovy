import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		concurrentBuild(false)
		label(meta.label)
		scm {
			git {
				remote { url('https://github.com/docker-library/postgres.git') }
				branches('*/master')
				extensions {
					cleanAfterCheckout()
				}
			}
		}
		triggers {
			//upstream("docker-${arch}-debian", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta, ['dpkgArch', 'gnuArch']) + '''
sed -i "s!^FROM !FROM $prefix/!" */{,*/}Dockerfile

# explicitly set the gcc arch tuple to the arch of gcc from the build environment
# (this makes sure our armhf build on arm64 hardware builds an armhf postgres)
sed -i "s!/configure !/configure --build='$gnuArch' !g" */{,*/}Dockerfile

(
	set +x

	for df in */{,*/}Dockerfile; do
		dir="$(dirname "$df")"
		from="$(awk '$1 == "FROM" { print $2; exit }' "$df")"
		if ! docker inspect "$from" &> /dev/null; then
			echo >&2 "warning, '$from' does not exist; skipping $dir"
			rm -f "$df"
		fi
	done

	if ! wget --quiet --spider "http://apt.postgresql.org/pub/repos/apt/dists/jessie-pgdg/main/binary-$dpkgArch/Packages"; then
		echo >&2 "warning, upstream doesn't support '$dpkgArch'; skipping Debian variants"
		rm -f */Dockerfile
	fi
)

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"

for v in */; do
	v="${v%/}"
	if [ -e "$v/Dockerfile" ]; then
		docker build -t "$repo:$v" "$v"
		pushImages+=( "$repo:$v" )
		if [ "$v" = "$latest" ]; then
			docker tag "$repo:$v" "$repo:latest"
			pushImages+=( "$repo:latest" )
		fi
	fi
	variants=( alpine )
	for variant in "${variants[@]}"; do
		if [ -e "$v/$variant/Dockerfile" ]; then
			docker build -t "$repo:$v-$variant" "$v/$variant"
			pushImages+=( "$repo:$v-$variant" )
			if [ "$v" = "$latest" ]; then
				docker tag "$repo:$v-$variant" "$repo:$variant"
				pushImages+=( "$repo:$variant" )
			fi
		fi
	done
done
''' + multiarch.templatePush(meta))
		}
	}
}
