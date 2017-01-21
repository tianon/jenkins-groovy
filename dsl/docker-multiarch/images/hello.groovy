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
				remote { url('https://github.com/docker-library/hello-world.git') }
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
			shell(multiarch.templateArgs(meta, ['arch']) + '''
sed -i "s!^FROM !FROM $prefix/!" Dockerfile.build
echo "Hello from Docker on $arch!" > hello-world/greeting.txt

./update.sh

docker build -t "$repo" hello-world

docker images "$repo"

docker run --rm "$repo"
''' + multiarch.templatePush(meta))
		}
	}
}
