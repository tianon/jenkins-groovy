def arches = [
	'arm64',
	'armel',
	'armhf',
	'ppc64le',
	's390x',
]

for (arch in arches) {
	freeStyleJob("docker-${arch}-php") {
		description("""<a href="https://hub.docker.com/r/${arch}/php/" target="_blank">Docker Hub page (<code>${arch}/php</code>)</a>""")
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

case "\$prefix" in
	s390x)
		# bundled pcre is silly and fails to build on s390x (too old)
		sed -i 's!buildDeps="!buildDeps="libpcre3-dev !; s!/configure !/configure --with-libdir="lib/\$(gcc -print-multiarch)" --with-pcre-regex=/usr !g' */{,*/}Dockerfile
		;;
esac

latest="\$(./generate-stackbrew-library.sh | awk '\$1 == "latest:" { print \$3; exit }')"

for v in */; do
	v="\${v%/}"
	docker build -t "\$repo:\$v" "\$v"
	docker tag -f "\$repo:\$v" "\$repo:\$v-cli"
	docker build -t "\$repo:\$v-apache" "\$v/apache"
	docker build -t "\$repo:\$v-fpm" "\$v/fpm"
	if [ "\$v" = "\$latest" ]; then
		docker tag -f "\$repo:\$v" "\$repo"
		docker tag -f "\$repo:\$v-cli" "\$repo:cli"
		docker tag -f "\$repo:\$v-apache" "\$repo:apache"
		docker tag -f "\$repo:\$v-fpm" "\$repo:fpm"
	fi
done

# we don't have /u/arm64
if [ "\$prefix" != 'arm64' ]; then
	docker images "\$repo" \\
		| awk -F '  +' 'NR>1 { print \$1 ":" \$2 }' \\
		| xargs -rtn1 docker push
fi
""")
		}
	}
}
