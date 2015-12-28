def arches = [
	'arm64',
	'armel',
	'armhf',
	'ppc64le',
	//'s390x', // TODO gosu for s390x?
]

for (arch in arches) {
	freeStyleJob("docker-${arch}-redis") {
		logRotator { daysToKeep(30) }
		label("docker-${arch}")
		scm {
			git {
				remote { url('https://github.com/docker-library/redis.git') }
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
repo="\$prefix/redis"

rm -r */32bit # explicit 32bit images don't make sense outside amd64

sed -i "s!^FROM !FROM \$prefix/!" */Dockerfile

latest="\$(./generate-stackbrew-library.sh | awk '\$1 == "latest:" { print \$3; exit }')"

for v in */; do
	v="\${v%/}"
	docker build -t "\$repo:\$v" "\$v"
	if [ "\$v" = "\$latest" ]; then
		docker tag -f "\$repo:\$v" "\$repo"
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
