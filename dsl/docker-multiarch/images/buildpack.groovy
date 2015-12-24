def arches = [
	'arm64',
	'armel',
	'armhf',
	'ppc64le',
	's390x',
]

for (arch in arches) {
	freeStyleJob("docker-${arch}-buildpack") {
		logRotator { daysToKeep(30) }
		label("docker-${arch}")
		scm {
			git {
				remote { url('https://github.com/docker-library/buildpack-deps.git') }
				branches('*/master')
				clean()
			}
		}
		triggers {
			upstream("docker-${arch}-debian, docker-${arch}-ubuntu", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell("""\
prefix='${arch}'
repo="\$prefix/buildpack-deps"

sed -i "s!^FROM !FROM \$prefix/!" Dockerfile.template*
./update.sh

for v in */; do
	v="\${v%/}"
	if \\
		! docker inspect "\$prefix/debian:\$v" &> /dev/null \\
		&& ! docker inspect "\$prefix/ubuntu:\$v" &> /dev/null \\
	; then
		echo >&2 "warning, neither '\$prefix/debian:\$v' or '\$prefix/ubuntu:\$v' exist; skipping \$v "
		rm -r "\$v/"
	fi
done

latest="\$(./generate-stackbrew-library.sh | awk '\$1 == "latest:" { print \$3; exit }')"
failed=0
for v in */; do
	v="\${v%/}"
	if \\
		docker build -t "\$repo:\$v-curl" "\$v/curl" \\
		&& docker build -t "\$repo:\$v-scm" "\$v/scm" \\
		&& docker build -t "\$repo:\$v" "\$v"; then
		if [ "\$v" = "\$latest" ]; then
			docker tag -f "\$repo:\$v-curl" "\$repo:curl"
			docker tag -f "\$repo:\$v-scm" "\$repo:scm"
			docker tag -f "\$repo:\$v" "\$repo"
		fi
	else
		echo >&2 "Failed to build some part of \$v; moving on."
		(( failed++ )) || true
	fi
done

# we don't have /u/arm64
if [ "\$prefix" != 'arm64' ]; then
	docker images "\$repo" \\
		| awk -F '  +' 'NR>1 { print \$1 ":" \$2 }' \\
		| xargs -rtn1 docker push
fi

exit "\$failed"
""")
		}
	}
}
