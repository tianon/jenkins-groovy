def arches = [
	'arm64',
	'armel',
	'armhf',
	'ppc64le',
	's390x',
]

for (arch in arches) {
	freeStyleJob("docker-${arch}-perl") {
		logRotator { daysToKeep(30) }
		label("docker-${arch}")
		scm {
			git {
				remote { url('https://github.com/perl/docker-perl.git') }
				branches('*/master')
				clean()
			}
		}
		triggers {
			upstream("docker-${arch}-buildpack", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell("""\
prefix='${arch}'
repo="\$prefix/perl"

sed -i "s!^FROM !FROM \$prefix/!" */Dockerfile
sed -i 's! -Duse64bitall!!g' */Dockerfile

#latest="\$(./generate-stackbrew-library.sh | awk '\$1 == "latest:" { print \$3; exit }')" # TODO calculate "latest" somehow
latest='5.022.001-64bit'

for f in */; do
	f="\${f%/}"
	var="\${f##*,}" # "threaded"
	[ "\$f" != "\$var" ] || var=
	major="\${f%%.*}" # "5"
	minor="\${f#\$major.}"
	minor="\${minor%%.*}" # "022"
	patch="\${f#\$major.\$minor.}"
	patch="\${patch%%-*}" # "001"
	minor="\${minor//0/}" # "22"
	patch="\${minor//0/}" # "1"
	v="\$major.\$minor.\$patch\${var:+-\$var}"
	docker build -t "\$repo:\$v" "\$f"
	if [ "\$f" = "\$latest" ]; then
		docker tag -f "\$repo:\$v" "\$repo"
	elif [ "\$var" -a "\$f" = "\$latest-\$var" ]; then
		docker tag -f "\$repo:\$v" "\$repo:\$var"
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
