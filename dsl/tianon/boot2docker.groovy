def releaseTypes = [:]
for (branch in ['master', '18.09.x']) {
	releaseTypes['tianon-boot2docker-' + branch] = [
		'branch': branch,
		'description': 'Builds an official <a href="https://github.com/boot2docker/boot2docker/releases">release-ready</a> ISO of <a href="https://github.com/boot2docker/boot2docker/blob/' + branch + '/VERSION">https://github.com/boot2docker/boot2docker/blob/' + branch + '/VERSION</a>',
		'shell': '''\
git-set-mtimes

dockerVersion="$(cat VERSION 2>/dev/null || awk '$1 == "ENV" && $2 == "DOCKER_VERSION" { print $3 }' Dockerfile)"

git log -1 > "version-GA-$dockerVersion"

targetImage="boot2docker/boot2docker:$dockerVersion"

docker build -t "$targetImage" --pull .
docker run --rm "$targetImage" > boot2docker.iso

docker push "$targetImage"
''' + (branch == 'master' ? '''
docker tag "$targetImage" boot2docker/boot2docker:latest
docker push boot2docker/boot2docker:latest

docker run --rm boot2docker/boot2docker tar -cC /tmp/stats . | tar -xv
{
	echo '<hr /><pre>'
	cat state.md
	echo '</pre><hr /><pre>'
	cat sums.md
	echo '</pre><hr />'
} > build-stats.html
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
				pattern('*.md')
				pattern('*.html')
				exclude('README.md,FAQ.md')
			}
		}
	}
}
