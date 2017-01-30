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
				remote { url('https://github.com/docker-library/tomcat.git') }
				branches('*/master')
				extensions {
					cleanAfterCheckout()
				}
			}
		}
		triggers {
			//upstream("docker-${arch}-openjdk", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta) + '''
sed -i "s!^FROM !FROM $prefix/!" */*/Dockerfile

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"

for v in */; do
	v="${v%/}"
	latestV="$(./generate-stackbrew-library.sh | awk '$1 == "'"$v"':" { print $3; exit }')"
	for variant in "$v"/*/; do
		variant="$(basename "$variant")"
		docker build -t "$repo:$v-$variant" "$v/$variant"
		if [ "$v/$variant" = "$latestV" ]; then
			docker tag "$repo:$v-$variant" "$repo:$v"
		fi
		if [ "$v/$variant" = "$latest" ]; then
			docker tag "$repo:$v-$variant" "$repo"
		fi
	done
done
''' + multiarch.templatePush(meta))
		}
	}
}
