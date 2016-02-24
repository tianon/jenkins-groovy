import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
		scm {
			git {
				remote { url('https://github.com/docker-library/python.git') }
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
			shell(multiarch.templateArgs(meta, ['dpkgArch']) + '''
if [[ "$dpkgArch" == arm* ]]; then
	rm -r 3.3
fi

sed -i "s!^FROM !FROM $prefix/!" */{,*/}Dockerfile

(
	set +x
	for df in */*/Dockerfile; do
		dir="$(dirname "$df")"
		from="$(awk '$1 == "FROM" { print $2; exit }' "$df")"
		if ! docker inspect "$from" &> /dev/null; then
			echo >&2 "warning, '$from' does not exist; skipping $dir"
			rm -r "$dir/"
		fi
	done
)

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"

for v in */; do
	v="${v%/}"
	docker build -t "$repo:$v" "$v"
	variants=( onbuild slim alpine )
	for variant in "${variants[@]}"; do
		if [ -d "$v/$variant" ]; then
			docker build -t "$repo:$v-$variant" "$v/$variant"
		fi
	done
	if [ "$v" = "$latest" ]; then
		docker tag -f "$repo:$v" "$repo"
		for variant in "${variants[@]}"; do
			from="$repo:$v-$variant"
			if docker inspect "$from" &> /dev/null; then
				docker tag -f "$from" "$repo:$variant"
			fi
		done
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
