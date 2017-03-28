def releaseTypes = [:]
for (branch in ['master', '17.03.x']) {
	releaseTypes['tianon-boot2docker-ga-' + branch] = [
		'branch': branch,
		'description': 'Builds an official release-ready ISO of <a href="https://github.com/boot2docker/boot2docker/blob/' + branch + '/VERSION">https://github.com/boot2docker/boot2docker/blob/' + branch + '/VERSION</a>',
		'shell': '''\
git-set-mtimes

dockerVersion="$(< VERSION)"

git log -1 > "version-GA-$dockerVersion"

docker build -t boot2docker/boot2docker --pull .
docker run --rm boot2docker/boot2docker > boot2docker.iso

docker push boot2docker/boot2docker:latest
docker tag boot2docker/boot2docker:latest "boot2docker/boot2docker:$dockerVersion"
docker push "boot2docker/boot2docker:$dockerVersion"
''',
	]
	releaseTypes['tianon-boot2docker-rc-' + branch] = [
		'branch': branch,
		'description': 'Builds an official RC-ready ISO of <a href="http://test.docker.com.s3.amazonaws.com/latest">http://test.docker.com.s3.amazonaws.com/latest</a>',
		'shell': '''\
git-set-mtimes

# TODO fix this to actually work the way I need it to for these non-master branches :(
dockerVersion="$(curl -fsSL 'http://test.docker.com.s3.amazonaws.com/latest')"
testDockerSha256="$(curl -fsSL "http://test.docker.com.s3.amazonaws.com/builds/Linux/x86_64/docker-${dockerVersion}.tgz.sha256" | cut -d' ' -f1)"

git log -1 > "version-TEST-$dockerVersion"

cat > Dockerfile.test <<EOD
FROM boot2docker/boot2docker

ENV TEST_DOCKER_VERSION ${dockerVersion}
ENV TEST_DOCKER_SHA256 ${testDockerSha256}

RUN set -x \\
	&& curl -fSL http://test.docker.com.s3.amazonaws.com/builds/Linux/x86_64/docker-\\$TEST_DOCKER_VERSION.tgz -o /tmp/dockerbin.tgz \\
	&& echo "\\${TEST_DOCKER_SHA256} */tmp/dockerbin.tgz" | sha256sum -c - \\
	&& tar -zxvf /tmp/dockerbin.tgz -C "\\$ROOTFS/usr/local/bin" --strip-components=1 \\
	&& rm /tmp/dockerbin.tgz \\
	&& chroot "\\$ROOTFS" docker -v \\
	&& echo "\\$TEST_DOCKER_VERSION" > "\\$ROOTFS/etc/version" \\
	&& cp -v "\\$ROOTFS/etc/version" /tmp/iso/version

RUN { echo; echo "  WARNING: this is a build from test.docker.com, not a stable release."; echo; } >> "\\$ROOTFS/etc/motd"

RUN /tmp/make_iso.sh
EOD

docker build -t boot2docker/boot2docker --pull .
docker build -t boot2docker/boot2docker:test -f Dockerfile.test .
docker run --rm boot2docker/boot2docker:test > boot2docker.iso

docker push boot2docker/boot2docker:latest
docker push boot2docker/boot2docker:test
docker tag boot2docker/boot2docker:test "boot2docker/boot2docker:$dockerVersion"
docker push "boot2docker/boot2docker:$dockerVersion"
''',
	]
}

for (releaseType in releaseTypes) {
	freeStyleJob(releaseType.key) {
		description(releaseType.value['description'])
		logRotator { numToKeep(5) }
		concurrentBuild(false)
		label('tianon-nameless')
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
