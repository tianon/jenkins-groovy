import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
		scm {
			git {
				remote { url('https://github.com/docker-library/drupal.git') }
				branches('*/master')
				extensions {
					cleanAfterCheckout()
				}
			}
		}
		triggers {
			upstream("docker-${arch}-php", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta) + '''
sed -i "s!^FROM !FROM $prefix/!" */*/Dockerfile

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"

for v in */; do
	v="${v%/}"
	docker build -t "$repo:$v-apache" "$v/apache"
	docker build -t "$repo:$v-fpm" "$v/fpm"
	docker tag -f "$repo:$v-apache" "$repo:$v"
	if [ "$v" = "$latest" ]; then
		docker tag -f "$repo:$v-apache" "$repo:apache"
		docker tag -f "$repo:$v-fpm" "$repo:fpm"
		docker tag -f "$repo:$v" "$repo"
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
