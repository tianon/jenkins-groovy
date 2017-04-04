import vars.multiarch

for (arch in multiarch.allArches([
	// no upstream binaries available
	'armel',
	'arm64',
])) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		concurrentBuild(false)
		label(meta.label)
		scm {
			git {
				remote { url('https://github.com/docker-library/golang.git') }
				branches('*/master')
				extensions {
					cleanAfterCheckout()
				}
			}
		}
		triggers {
			//upstream("docker-${arch}-buildpack", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta, ['dpkgArch']) + '''
fromArch='linux-amd64'
case "$dpkgArch" in
	armhf)   toArch='linux-armv6l' ;;
	i386)    toArch='linux-386' ;;
	ppc64el) toArch='linux-ppc64le' ;;
	s390x)   toArch='linux-s390x' ;;
	*)
		echo >&2 "unsupported architecture: $dpkgArch ($prefix)"
		exit 1
		;;
esac

sed -i "s!$fromArch!$toArch!g" */{,*/}Dockerfile update.sh
./update.sh # update sha256s
sed -i "s!^FROM !FROM $prefix/!" */{,*/}Dockerfile

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"

for df in */{,*/}Dockerfile; do
	v="$(dirname "$df")"
	if [[ "$v" == *onbuild* ]]; then
		# onbuild needs to be built separately
		continue
	fi

	from="$(awk -F '[[:space:]]+' '$1 == "FROM" { print $2; exit }' "$df")"
	if ! docker inspect "$from" &> /dev/null; then
		cat >&2 <<-EOF


			warning: '$from' does not exist
				skipping $v


		EOF
		echo >&2 "warning, '$from' does not exist; skipping $v"
		rm -r "$v/"
		continue
	fi

	fullVersion="$(awk -F '[[:space:]]+' '$1 == "ENV" && $2 == "GOLANG_VERSION" { print $3; exit }' "$df")"
	[ -n "$fullVersion" ]

	url="https://golang.org/dl/go${fullVersion}.${toArch}.tar.gz"
	if ! wget --spider --quiet "$url"; then
		cat >&2 <<-EOF


			warning: $url not found
			    skipping $v


		EOF
		continue
	fi

	slash='/'
	tag="${v//$slash/-}"
	docker build -t "$repo:$tag" "$v"
	pushImages+=( "$repo:$tag" )
	docker tag "$repo:$tag" "$repo:$fullVersion"
	pushImages+=( "$repo:$fullVersion" )
	if [ "$v" = "$latest" ]; then
		docker tag "$repo:$tag" "$repo:latest"
		pushImages+=( "$repo:latest" )
	fi

	if [ -d "$v/onbuild" ]; then
		docker build -t "$repo:$tag-onbuild" "$v/onbuild"
		pushImages+=( "$repo:$tag-onbuild" )
		docker tag "$repo:$tag-onbuild" "$repo:$fullVersion-onbuild"
		pushImages+=( "$repo:$fullVersion-onbuild" )
		if [ "$v" = "$latest" ]; then
			docker tag "$repo:$tag-onbuild" "$repo:onbuild"
			pushImages+=( "$repo:onbuild" )
		fi
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
