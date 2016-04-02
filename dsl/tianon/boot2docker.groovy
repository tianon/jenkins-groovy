def releaseTypes = [
	'tianon-boot2docker-ga': [
		'description': 'Builds an official release-ready ISO of <a href="https://github.com/boot2docker/boot2docker/blob/master/VERSION">https://github.com/boot2docker/boot2docker/blob/master/VERSION</a>',
		'shell': '''\
git-set-mtimes

touch "version-GA-$(< VERSION)"

docker build -t boot2docker/boot2docker --pull .
docker run --rm boot2docker/boot2docker > boot2docker.iso
''',
	],
	'tianon-boot2docker-rc': [
		'description': 'Builds an official RC-ready ISO of <a href="http://test.docker.com.s3.amazonaws.com/latest">http://test.docker.com.s3.amazonaws.com/latest</a>',
		'shell': '''\
git-set-mtimes

testDocker="$(curl -fsSL 'http://test.docker.com.s3.amazonaws.com/latest')"
testDockerSha256="$(curl -fsSL "http://test.docker.com.s3.amazonaws.com/builds/Linux/x86_64/docker-${testDocker}.tgz.sha256" | cut -d' ' -f1)"

touch "version-TEST-$testDocker"

cat > Dockerfile.test <<EOD
FROM boot2docker/boot2docker

ENV TEST_DOCKER_VERSION ${testDocker}
ENV TEST_DOCKER_SHA256 ${testDockerSha256}

RUN set -x \\
	&& curl -fSL http://test.docker.com.s3.amazonaws.com/builds/Linux/x86_64/docker-\\$TEST_DOCKER_VERSION.tgz -o /tmp/dockerbin.tgz \\
	&& echo "\\${TEST_DOCKER_SHA256} */tmp/dockerbin.tgz" | sha256sum -c - \\
	&& tar -zxvf /tmp/dockerbin.tgz -C "\\$ROOTFS/usr/local/bin" --strip-components=1 \\
	&& rm /tmp/dockerbin.tgz \\
	&& chroot "\\$ROOTFS" docker -v

RUN \\$ROOTFS/usr/local/bin/docker -v | sed -r 's/.* version ([^ ,]+).*/\\1/' > \\$ROOTFS/etc/version \\
	&& cp -v \\$ROOTFS/etc/version /tmp/iso/version

RUN { echo; echo "  WARNING: this is a test.docker.com build, not a release."; echo; } >> \\$ROOTFS/etc/motd

RUN /make_iso.sh
EOD

docker build -t boot2docker/boot2docker --pull .
docker build -t boot2docker/boot2docker:test -f Dockerfile.test .
docker run --rm boot2docker/boot2docker:test > boot2docker.iso
''',
	],
]

for (releaseType in releaseTypes) {
	freeStyleJob(releaseType.key) {
		description(releaseType.value['description'])
		logRotator { numToKeep(5) }
		label('tianon-nameless')
		scm {
			git {
				remote {
					url('https://github.com/boot2docker/boot2docker.git')
				}
				branches('*/master')
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
				pattern('boot2docker.iso')
				pattern('version-*')
			}
		}
	}
}
