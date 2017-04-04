import vars.multiarch

def images = [
	'alpine',
	'buildpack-deps',
	'busybox',
	'debian',
	'drupal',
	'erlang',
	'fedora',
	'gcc',
	'golang',
	'haproxy',
	'hello-world',
	'httpd',
	'irssi',
	'maven',
	'memcached',
	'neo4j',
	'node',
	'openjdk',
	'opensuse',
	'perl',
	'php',
	'postgres',
	'pypy',
	'python',
	'r-base',
	'rakudo-star',
	'redis',
	'ruby',
	'solr',
	'tomcat',
	'ubuntu',
	'wordpress',
]

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		logRotator { daysToKeep(30) }
		concurrentBuild(false)
		label(meta.label)
		scm {
			git {
				remote { url('https://github.com/docker-library/docs.git') }
				branches('*/master')
				extensions {
					cleanAfterCheckout()
				}
			}
		}
		triggers {
			upstream("docker-${arch}-perl, docker-${arch}-hello", 'UNSTABLE')
			scm('H/6 * * * *')
		}
		wrappers {
			colorizeOutput()
			credentialsBinding {
				usernamePassword('USERNAME', 'PASSWORD', "docker-hub-buildslave${arch}")
			}
		}
		steps {
			shell(multiarch.templateArgs(meta, [], ['prefix']) + """
images=(
	${images.join("\n\t")}
)""" + '''
archImages=()
set +x
for image in "${images[@]}"; do
	archImages+=( "$prefix/$image" )
done
echo "archImages=( ${archImages[*]} )"
set -x

sed -i "s!^FROM !FROM $prefix/!" Dockerfile
cat > .template-helpers/generate-dockerfile-links-partial.sh <<-'EOF'
	#!/bin/bash
	set -e
	
	echo '** THESE IMAGES ARE VERY EXPERIMENTAL; THEY ARE PROVIDED ON A BEST-EFFORT BASIS WHILE [docker-library/official-images#2289](https://github.com/docker-library/official-images/issues/2289) IS STILL IN-PROGRESS (which is the first step towards proper multiarch images) **'
	echo
	echo '** PLEASE DO NOT USE THEM FOR IMPORTANT THINGS **'
	echo
	
	echo "This image is built from the source of the [official image of the same name (\\`$1\\`)](https://hub.docker.com/_/$1/).  Please see that image's description for links to the relevant \\`Dockerfile\\`s."
	echo
	
	echo 'If you are curious about specifically how this image differs, see [the Jenkins Groovy DSL scripts in the `tianon/jenkins-groovy` GitHub repository](https://github.com/tianon/jenkins-groovy/tree/master/dsl/docker-multiarch/images), which are responsible for creating the Jenkins jobs which build them.'
	echo
EOF
cat > .template-helpers/user-feedback.md <<-'EOF'
	If you have issues with or suggestions for this image, please file them as issues on the [`tianon/jenkins-groovy` GitHub repository](https://github.com/tianon/jenkins-groovy/issues).
EOF
sed -ri "s!^docker pull !#&!; s!^(docker run --rm|docker images) !\\1 $prefix/!" hello-world/update.sh
./update.sh "${images[@]}"

docker build -t "docker-library-docs:$prefix" .
test -t 1 && it='-it' || it='-i'
set +x
docker run "$it" --rm -e TERM \\
	--entrypoint './push.pl' \\
	"docker-library-docs:$prefix" \\
	--username "$USERNAME" \\
	--password "$PASSWORD" \\
	--batchmode \\
		"${archImages[@]}"
''')
		}
	}
}
