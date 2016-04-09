import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
		scm {
			git {
				remote { url('https://github.com/neo4j/docker-neo4j.git') }
				branches('*/master')
				clean()
			}
		}
		triggers {
			upstream("docker-${arch}-openjdk", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta) + '''
sed -i "s!^FROM !FROM $prefix/!" */Dockerfile

latest='2.3.2' # TODO calculate "latest" somehow

for v in */; do
	v="${v%/}"
	docker build -t "$repo:$v" "$v"
	if [ "$v" = "$latest" ]; then
		docker tag -f "$repo:$v" "$repo"
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
