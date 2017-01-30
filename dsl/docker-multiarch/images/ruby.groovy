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
				remote { url('https://github.com/docker-library/ruby.git') }
				branches('*/master')
				extensions {
					cleanAfterCheckout()
				}
			}
		}
		triggers {
			//upstream("docker-${arch}-debian, docker-${arch}-buildpack", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta) + '''
sed -i "s!^FROM !FROM $prefix/!" */{,*/}Dockerfile

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"

for v in */; do
	v="${v%/}"
	docker build -t "$repo:$v" "$v"
	docker build -t "$repo:$v-onbuild" "$v/onbuild"
	docker build -t "$repo:$v-slim" "$v/slim"
	if [ "$v" = "$latest" ]; then
		docker tag "$repo:$v" "$repo"
		docker tag "$repo:$v-onbuild" "$repo:onbuild"
		docker tag "$repo:$v-slim" "$repo:slim"
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
