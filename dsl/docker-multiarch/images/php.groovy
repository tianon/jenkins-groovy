def arches = [
	'arm64',
	'armel',
	'armhf',
	'ppc64le',
]

for (arch in arches) {
	freeStyleJob("docker-${arch}-php") {
		logRotator { daysToKeep(30) }
		label("docker-${arch}")
		scm {
			git {
				remote { url('https://github.com/docker-library/php.git') }
				branches('*/master')
				clean()
			}
		}
		triggers {
			upstream("docker-${arch}-debian", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell("""\
prefix='${arch}'
repo="\$prefix/php"

sed -i "s!^FROM !FROM \$prefix/!" */{,*/}Dockerfile

latest="\$(./generate-stackbrew-library.sh | awk '\$1 == "latest:" { print \$3; exit }')"

for v in */; do
	v="\${v%/}"
	docker build -t "\$repo:\$v" "\$v"
	docker tag -f "\$repo:\$v" "\$repo:\$v-cli"
	docker build -t "\$repo:\$v-apache" "\$v/apache"
	docker build -t "\$repo:\$v-fpm" "\$v/fpm"
	if [ "\$v" = 'latest' ]; then
		docker tag -f "\$repo:\$v" "\$repo"
		docker tag -f "\$repo:\$v-cli" "\$repo:cli"
		docker tag -f "\$repo:\$v-apache" "\$repo:apache"
		docker tag -f "\$repo:\$v-fpm" "\$repo:fpm"
	fi
done

# we don't have /u/arm64
if [ '${arch}' != 'arm64' ]; then
	docker images "\$repo" \\
		| awk -F '  +' 'NR>1 { print \$1 ":" \$2 }' \\
		| xargs -rtn1 docker push
fi
""")
		}
	}
}
