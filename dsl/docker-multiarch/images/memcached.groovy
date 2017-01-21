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
				remote { url('https://github.com/docker-library/memcached.git') }
				branches('*/master')
				extensions {
					cleanAfterCheckout()
				}
			}
		}
		triggers {
			upstream("docker-${arch}-debian", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta) + '''
sed -i "s!^FROM !FROM $prefix/!" */Dockerfile

(
	set +x
	for df in */Dockerfile; do
		dir="$(dirname "$df")"
		from="$(awk '$1 == "FROM" { print $2; exit }' "$df")"
		if ! docker inspect "$from" &> /dev/null; then
			echo >&2 "warning, '$from' does not exist; skipping $dir"
			rm -r "$dir/"
		fi
	done
)

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"

for variant in */; do
	variant="${variant%/}"
	docker build -t "$repo:$variant" "$variant"
	version="$(awk '$1 == "ENV" && $2 == "MEMCACHED_VERSION" { print $3; exit }' "$variant/Dockerfile")"
	docker tag "$repo:$variant" "$repo:$version-$variant"
	if [ "$variant" = "$latest" ]; then
		docker tag "$repo:$variant" "$repo"
		docker tag "$repo:$version-$variant" "$repo:$version"
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
