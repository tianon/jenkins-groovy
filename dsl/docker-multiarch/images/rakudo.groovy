import vars.multiarch

for (arch in multiarch.allArches([
	's390x', // not supported by "dyncall"
])) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
		scm {
			git {
				remote { url('https://github.com/perl6/docker.git') }
				branches('*/master')
				extensions {
					cleanAfterCheckout()
				}
			}
		}
		triggers {
			upstream("docker-${arch}-buildpack", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta) + '''
sed -i "s!^FROM !FROM $prefix/!" Dockerfile

docker build -t "$repo" .
version="$(awk -F '[ =]' '$1 == "ENV" && $2 == "rakudo_version" { print $3; exit }' Dockerfile)"
docker tag "$repo" "$repo:$version"
''' + multiarch.templatePush(meta))
		}
	}
}
