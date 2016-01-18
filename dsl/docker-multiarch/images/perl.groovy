import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
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
			shell(multiarch.templateArgs(meta, ['archBits', 'gnuArch']) + '''
sed -i "s!^FROM !FROM $prefix/!" */Dockerfile

# see https://sources.debian.net/src/perl/jessie/debian/config.debian/
if [ "$archBits" = '32' ]; then
	# *** You have chosen a maximally 64-bit build,
	# *** but your pointers are only 4 bytes wide.
	# *** Please rerun Configure without -Duse64bitall.
	# *** Since you have quads, you could possibly try with -Duse64bitint.
	sed -i 's! -Duse64bitall! -Duse64bitint!' */Dockerfile
fi
sed -i "s!Configure !Configure -Darchname='$gnuArch' !" */Dockerfile

#latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')" # TODO calculate "latest" somehow
latest='5.022.001-64bit'

for f in */; do
	f="${f%/}"
	var="${f##*,}" # "threaded"
	[ "$f" != "$var" ] || var=
	suff="${var:+-$var}"
	major="${f%%.*}" # "5"
	minor="${f#$major.}"
	minor="${minor%%.*}" # "022"
	patch="${f#$major.$minor.}"
	patch="${patch%%-*}" # "001"
	while [ "$minor" != "${minor#0}" ]; do minor="${minor#0}"; done # "22"
	while [ "$patch" != "${patch#0}" ]; do patch="${patch#0}"; done # "1"
	if [ "$major" -lt 5 ]; then continue; fi
	if [ "$minor" -lt 20 ]; then continue; fi
	v="$major.$minor.$patch$suff"
	docker build -t "$repo:$v" "$f"
	docker tag -f "$repo:$v" "$repo:$major.$minor$suff"
	if [ "$f" = "$latest" ]; then
		docker tag -f "$repo:$v" "$repo:$major"
		docker tag -f "$repo:$v" "$repo"
	elif [ "$var" -a "$f" = "$latest,$var" ]; then
		docker tag -f "$repo:$v" "$repo:$major$suff"
		docker tag -f "$repo:$v" "$repo:$var"
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
