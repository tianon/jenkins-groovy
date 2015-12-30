def arches = [
	'arm64',
	'armel',
	'armhf',
	'ppc64le',
	's390x',
]

for (arch in arches) {
	freeStyleJob("docker-${arch}-mono") {
		description("""<a href="https://hub.docker.com/r/${arch}/mono/" target="_blank">Docker Hub page (<code>${arch}/mono</code>)</a>""")
		logRotator { daysToKeep(30) }
		label("docker-${arch}")
		scm {
			git {
				remote { url('https://github.com/mono/docker.git') }
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
repo="\$prefix/mono"

sed -i "s!^FROM !FROM \$prefix/!" */Dockerfile

for v in */; do
	v="\${v%/}"
	from="\$(awk '\$1 == "FROM" { print \$2; exit }' "\$v/Dockerfile")"
	if ! docker inspect "\$from" &> /dev/null; then
		if [[ "\$from" == *:wheezy ]]; then
			# some of our arches aren't supported by wheezy, but mono's repo supports them (arm64, ppc64le)
			sed -i 's/:wheezy$/:jessie/g' "\$v/Dockerfile"
		else
			echo >&2 "warning, '\$from' does not exist; skipping \$v"
			rm -r "\$v/"
		fi
	fi
done

latest="\$(./generate-stackbrew-library.sh | awk '\$1 == "latest:" { print \$3; exit }')"

for v in */; do
	v="\${v%/}"
	docker build -t "\$repo:\$v" "\$v"
	docker build -t "\$repo:\$v-onbuild" "\$v/onbuild"
	if [ "\$v" = "\$latest" ]; then
		docker tag -f "\$repo:\$v" "\$repo"
		docker tag -f "\$repo:\$v-onbuild" "\$repo:onbuild"
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
