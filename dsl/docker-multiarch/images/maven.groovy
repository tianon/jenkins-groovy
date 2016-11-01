import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
		scm {
			git {
				remote { url('https://github.com/carlossg/docker-maven.git') }
				branches('*/master')
				extensions {
					cleanAfterCheckout()
				}
			}
		}
		triggers {
			upstream("docker-${arch}-openjdk", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta) + '''
rm -rf tests

sed -i "s!^FROM !FROM $prefix/!; s!:openjdk-!:!" */{,*/}Dockerfile

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"

for v in */; do
	v="${v%/}"
	docker build -t "$repo:$v" "$v"
	#docker build -t "$repo:$v-onbuild" "$v/onbuild"
	if [ "$v" = "$latest" ]; then
		docker tag "$repo:$v" "$repo"
		#docker tag "$repo:$v-onbuild" "$repo:onbuild"
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
