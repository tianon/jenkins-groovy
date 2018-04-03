def releaseTypes = [:]
for (branch in ['master', '18.03.x', '17.12.x']) {
	releaseTypes['tianon-boot2docker-' + branch] = [
		'branch': branch,
		'description': 'Builds an official release-ready ISO of <a href="https://github.com/boot2docker/boot2docker/blob/' + branch + '/VERSION">https://github.com/boot2docker/boot2docker/blob/' + branch + '/VERSION</a>',
		'shell': '''\
git-set-mtimes

dockerVersion="$(cat VERSION)"

git log -1 > "version-GA-$dockerVersion"

targetImage="boot2docker/boot2docker:$dockerVersion"

docker build -t "$targetImage" --pull .
docker run --rm "$targetImage" > boot2docker.iso

docker push "$targetImage"
''' + (branch == 'master' ? '''
docker tag "$targetImage" boot2docker/boot2docker:latest
docker push boot2docker/boot2docker:latest
''' : ''),
	]
}

for (releaseType in releaseTypes) {
	freeStyleJob(releaseType.key) {
		description(releaseType.value['description'])
		authorization {
			permission('hudson.model.Item.Read', 'anonymous')
		}
		logRotator { numToKeep(5) }
		concurrentBuild(false)
		label('tianon-zoe')
		scm {
			git {
				remote {
					url('https://github.com/boot2docker/boot2docker.git')
				}
				branches('*/' + releaseType.value['branch'])
				extensions {
					cleanAfterCheckout()
				}
			}
		}
		triggers {
			//scm('H/5 * * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(releaseType.value['shell'])
		}
		publishers {
			archiveArtifacts {
				fingerprint()
				pattern('boot2docker*.iso')
				pattern('version-*')
			}
		}
	}
}
