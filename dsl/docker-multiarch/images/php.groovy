import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
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
			shell(multiarch.templateArgs(meta, ['dpkgArch', 'gccArch']) + '''
sed -i "s!^FROM !FROM $prefix/!" */{,*/}Dockerfile

case "$dpkgArch" in
	s390x)
		# bundled pcre is silly and fails to build on s390x (too old)
		sed -i 's!buildDeps="!buildDeps="libpcre3-dev !' */{,*/}Dockerfile
		sed -i "s!/configure !/configure --with-libdir='lib/$gccArch' --with-pcre-regex=/usr !g" */{,*/}Dockerfile
		;;
esac

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"

for v in */; do
	v="${v%/}"
	docker build -t "$repo:$v" "$v"
	docker tag -f "$repo:$v" "$repo:$v-cli"
	docker build -t "$repo:$v-apache" "$v/apache"
	docker build -t "$repo:$v-fpm" "$v/fpm"
	if [ "$v" = "$latest" ]; then
		docker tag -f "$repo:$v" "$repo"
		docker tag -f "$repo:$v-cli" "$repo:cli"
		docker tag -f "$repo:$v-apache" "$repo:apache"
		docker tag -f "$repo:$v-fpm" "$repo:fpm"
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
