def releaseTypes = [
	'tianon-boot2docker-ga': [
		'description': 'Builds an official release-ready ISO of <a href="https://github.com/boot2docker/boot2docker/blob/master/VERSION">https://github.com/boot2docker/boot2docker/blob/master/VERSION</a>',
		'shell': '''\
git-set-mtimes

dockerVersion="$(< VERSION)"

git log -1 > "version-GA-$dockerVersion"

docker build -t boot2docker/boot2docker --pull .
docker run --rm boot2docker/boot2docker > boot2docker.iso
''',
	],
	'tianon-boot2docker-rc': [
		'description': 'Builds an official RC-ready ISO of <a href="http://test.docker.com.s3.amazonaws.com/latest">http://test.docker.com.s3.amazonaws.com/latest</a>',
		'shell': '''\
git-set-mtimes

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
	&& chroot "\\$ROOTFS" docker -v

RUN chroot "\\$ROOTFS" docker -v | sed -r 's/.* version ([^ ,]+).*/\\1/' > "\\$ROOTFS/etc/version" \\
	&& cp -v "\\$ROOTFS/etc/version" /tmp/iso/version

RUN { echo; echo "  WARNING: this is a build from test.docker.com, not a stable release."; echo; } >> "\\$ROOTFS/etc/motd"

RUN /make_iso.sh
EOD

docker build -t boot2docker/boot2docker --pull .
docker build -t boot2docker/boot2docker:test -f Dockerfile.test .
docker run --rm boot2docker/boot2docker:test > boot2docker.iso
''',
	],
]

def experimentalShell = '''
experimentalDockerSha256="$(curl -fsSL "http://experimental.docker.com.s3.amazonaws.com/builds/Linux/x86_64/docker-${dockerVersion}.tgz.sha256" | cut -d' ' -f1)"

cat > Dockerfile.experimental <<EOD
FROM boot2docker/boot2docker

ENV EXPERIMENTAL_DOCKER_VERSION ${dockerVersion}
ENV EXPERIMENTAL_DOCKER_SHA256 ${experimentalDockerSha256}

RUN set -x \\
	&& curl -fSL http://experimental.docker.com.s3.amazonaws.com/builds/Linux/x86_64/docker-\\$EXPERIMENTAL_DOCKER_VERSION.tgz -o /tmp/dockerbin.tgz \\
	&& echo "\\${EXPERIMENTAL_DOCKER_SHA256} */tmp/dockerbin.tgz" | sha256sum -c - \\
	&& tar -zxvf /tmp/dockerbin.tgz -C "\\$ROOTFS/usr/local/bin" --strip-components=1 \\
	&& rm /tmp/dockerbin.tgz \\
	&& chroot "\\$ROOTFS" docker -v

RUN chroot "\\$ROOTFS" docker -v | sed -r 's/.* version ([^ ,]+).*/\\1/' > "\\$ROOTFS/etc/version" \\
	&& cp -v "\\$ROOTFS/etc/version" /tmp/iso/version

RUN { echo; echo "  WARNING: this is a build from experimental.docker.com, not a stable release."; echo; } >> "\\$ROOTFS/etc/motd"

RUN /make_iso.sh
EOD

docker build -t boot2docker/boot2docker:experimental -f Dockerfile.experimental .
docker run --rm boot2docker/boot2docker:experimental > boot2docker-experimental.iso
'''

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
			shell(releaseType.value['shell'] + experimentalShell)
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
