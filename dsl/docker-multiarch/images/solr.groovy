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
				remote { url('https://github.com/docker-solr/docker-solr.git') }
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
rm -rf docs scripts

sed -ri "s!^FROM +!FROM $prefix/!" */Dockerfile

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"

for v in */; do
	v="${v%/}"
	docker build -t "$repo:$v" "$v"
	if [ "$v" = "$latest" ]; then
		docker tag "$repo:$v" "$repo"
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
