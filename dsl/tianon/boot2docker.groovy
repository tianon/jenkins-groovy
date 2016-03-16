for (type in ['ga', 'rc']) {
	script = '''\
git-set-mtimes
docker build -t boot2docker/boot2docker --pull .
'''
	switch (type) {
		case 'ga':
			script += '''\
touch "version-$(< VERSION)"

docker run --rm boot2docker/boot2docker > boot2docker.iso
'''
			break
		case 'rc':
			script += '''\
testDocker="$(curl -fsSL 'http://test.docker.com.s3.amazonaws.com/latest')"
testDockerSha256="$(curl -fsSL "http://test.docker.com.s3.amazonaws.com/builds/Linux/x86_64/docker-${testDocker}.sha256" | cut -d' ' -f1)"

touch "version-$testDocker"

cat > Dockerfile.test <<EOD
FROM boot2docker/boot2docker

ENV TEST_DOCKER_VERSION ${testDocker}
ENV TEST_DOCKER_SHA256 ${testDockerSha256}

RUN set -x \
	&& curl -fsSL http://test.docker.com.s3.amazonaws.com/builds/Linux/x86_64/docker-\\$TEST_DOCKER_VERSION -o \\$ROOTFS/usr/local/bin/docker \
	&& echo "\\${TEST_DOCKER_SHA256} *\\$ROOTFS/usr/local/bin/docker" | sha256sum -c - \
	&& chmod +x \\$ROOTFS/usr/local/bin/docker \
	&& \\$ROOTFS/usr/local/bin/docker -v

RUN \\$ROOTFS/usr/local/bin/docker -v | sed -r 's/.* version ([^ ,]+).*/\1/' > \\$ROOTFS/etc/version \
	&& cp -v \\$ROOTFS/etc/version /tmp/iso/version

RUN { echo; echo "  WARNING: this is a test.docker.com build, not a release."; echo; } >> \\$ROOTFS/etc/motd

RUN /make_iso.sh
EOD

docker build -t boot2docker/boot2docker:test -f Dockerfile.test .
docker run --rm boot2docker/boot2docker:test > boot2docker.iso
'''
	}

	freeStyleJob('tianon-boot2docker-' + type) {
		logRotator { numToKeep(5) }
		label('tianon-nameless')
		scm {
			git {
				remote {
					url('https://github.com/boot2docker/boot2docker.git')
				}
				branches('*/master')
				clean()
			}
		}
		triggers {
			//scm('H/5 * * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(script)
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
