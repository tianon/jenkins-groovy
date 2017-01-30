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
				remote { url('https://github.com/docker-library/openjdk.git') }
				branches('*/master')
				extensions {
					cleanAfterCheckout()
				}
			}
		}
		triggers {
			//upstream("docker-${arch}-buildpack", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta, ['dpkgArch']) + '''
sed -i "s!^FROM !FROM $prefix/!" */Dockerfile
sed -i "s!amd64!$dpkgArch!g" */Dockerfile

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"

for v in */; do
	v="${v%/}"
	if ! docker build -t "$repo:$v" "$v"; then
		echo >&2 "warning: building '$v' failed; skipping"
		continue
	fi
	docker tag "$repo:$v" "$repo:openjdk-$v"
	if [ "$v" = "$latest" ]; then
		docker tag "$repo:$v" "$repo"
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
