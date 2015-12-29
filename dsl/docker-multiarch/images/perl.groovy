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

# see https://sources.debian.net/src/perl/jessie/debian/config.debian/
sed -i "s!Configure !Configure -Darchname=\$prefix-linux !" */Dockerfile

case "\$prefix" in
	ppc64le|s390x)
		#sed -i '/make test_harness/d' */Dockerfile
		;;
esac

#latest="\$(./generate-stackbrew-library.sh | awk '\$1 == "latest:" { print \$3; exit }')" # TODO calculate "latest" somehow
latest='5.022.001-64bit'

for f in */; do
	f="\${f%/}"
	var="\${f##*,}" # "threaded"
	[ "\$f" != "\$var" ] || var=
	suff="\${var:+-\$var}"
	major="\${f%%.*}" # "5"
	minor="\${f#\$major.}"
	minor="\${minor%%.*}" # "022"
	patch="\${f#\$major.\$minor.}"
	patch="\${patch%%-*}" # "001"
	while [ "\$minor" != "\${minor#0}" ]; do minor="\${minor#0}"; done # "22"
	while [ "\$patch" != "\${patch#0}" ]; do patch="\${patch#0}"; done # "1"
	if [ "\$major" -lt 5 ]; then continue; fi
	if [ "\$minor" -lt 20 ]; then continue; fi
	v="\$major.\$minor.\$patch\$suff"
	docker build -t "\$repo:\$v" "\$f"
	docker tag -f "\$repo:\$v" "\$repo:\$major.\$minor\$suff"
	if [ "\$f" = "\$latest" ]; then
		docker tag -f "\$repo:\$v" "\$repo:\$major"
		docker tag -f "\$repo:\$v" "\$repo"
	elif [ "\$var" -a "\$f" = "\$latest,\$var" ]; then
		docker tag -f "\$repo:\$v" "\$repo:\$major\$suff"
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
