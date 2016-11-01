import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
		scm {
			git {
				remote { url('https://github.com/rocker-org/rocker.git') }
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
cd r-base

sed -i "s!^FROM !FROM $prefix/!" Dockerfile

docker build -t "$repo" .
version="$(awk '$1 == "ENV" && $2 == "R_BASE_VERSION" { print $3; exit }' Dockerfile)"
docker tag "$repo" "$repo:$version"
''' + multiarch.templatePush(meta))
		}
	}
}
