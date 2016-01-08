import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
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
			shell(multiarch.templateArgs(meta) + '''
sed -i "s!^FROM !FROM $prefix/!" Dockerfile.template*
./update.sh

(
	set +x
	for v in */; do
		v="${v%/}"
		if \\
			! docker inspect "$prefix/debian:$v" &> /dev/null \\
			&& ! docker inspect "$prefix/ubuntu:$v" &> /dev/null \\
		; then
			echo >&2 "warning, neither '$prefix/debian:$v' or '$prefix/ubuntu:$v' exist; skipping $v "
			rm -r "$v/"
		fi
	done
)

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"
failed=0
for v in */; do
	v="${v%/}"
	if \\
		docker build -t "$repo:$v-curl" "$v/curl" \\
		&& docker build -t "$repo:$v-scm" "$v/scm" \\
		&& docker build -t "$repo:$v" "$v"; then
		if [ "$v" = "$latest" ]; then
			docker tag -f "$repo:$v-curl" "$repo:curl"
			docker tag -f "$repo:$v-scm" "$repo:scm"
			docker tag -f "$repo:$v" "$repo"
		fi
	else
		echo >&2 "Failed to build some part of $v; moving on."
		(( failed++ )) || true
	fi
done
''' + multiarch.templatePush(meta) + '''
exit "$failed"
''')
		}
	}
}
