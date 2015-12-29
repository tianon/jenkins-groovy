def arches = [
	//'arm64',
	//'armel',
	'armhf',
	//'ppc64le',
	//'s390x',
]

def images = [
	//'busybox',
	//'debian',
	'hello-world',
	//'ubuntu',
]

for (arch in arches) {
	archImages = images.collect { arch + '/' + it }
	freeStyleJob("docker-${arch}-docs") {
		logRotator { daysToKeep(30) }
		label("docker-${arch}")
		scm {
			git {
				remote { url('https://github.com/docker-library/docs.git') }
				branches('*/master')
				clean()
			}
		}
		triggers {
			upstream("docker-${arch}-perl", 'UNSTABLE')
			scm('H/6 * * * *')
		}
		wrappers {
			colorizeOutput()
			credentialsBinding {
				usernamePassword('USERNAME', 'PASSWORD', "docker-hub-buildslave${arch}")
			}
		}
		steps {
			shell("""\
prefix='${arch}'

sed -i "s!^FROM !FROM \$prefix/!" Dockerfile
cat > .template-helpers/generate-dockerfile-links-partial.sh <<-'EOF'
	#!/bin/bash
	set -e
	
	echo "This image is built from the source of the [official image of the same name (\\`\$1\\`)](https://hub.docker.com/_/\$1/).  Please see that image's description for links to the relevant \\`Dockerfile\\`s."
	echo
	
	echo 'If you are curious about specifically how these images are built, see [the Jenkins Groovy DSL scripts in the `tianon/jenkins-groovy` GitHub repository](https://github.com/tianon/jenkins-groovy/tree/master/dsl/docker-multiarch/images).'
	echo
EOF
sed -i 's!^docker pull !#&!' */update.sh
./update.sh \\
	${images.join(" \\\n\t")}

docker build --pull -t docker-library-docs .
test -t 1 && it='-it' || it='-i'
set +x
docker run "\$it" --rm -e TERM \\
	--entrypoint './push.pl' \\
	docker-library-docs \\
	--username "\$USERNAME" \\
	--password "\$PASSWORD" \\
	--batchmode \\
		${archImages.join(" \\\n\t\t")}
""")
		}
	}
}
