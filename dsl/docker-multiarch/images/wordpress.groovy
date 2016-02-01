import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
		scm {
			git {
				remote { url('https://github.com/docker-library/wordpress.git') }
				branches('*/master')
				clean()
			}
		}
		triggers {
			upstream("docker-${arch}-php", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta, ['dpkgArch']) + '''
sed -i "s!^FROM !FROM $prefix/!" */{,*/}Dockerfile

docker build -t "$repo:apache" "apache"
docker build -t "$repo:fpm" "fpm"
''' + multiarch.templatePush(meta))
		}
	}
}
