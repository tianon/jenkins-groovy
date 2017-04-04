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

sed -i "s!$fromArch!$toArch!g" */{,*/}Dockerfile
sed -i "s!^FROM !FROM $prefix/!" */{,*/}Dockerfile

(
	for df in */{,*/}Dockerfile; do
		dir="$(dirname "$df")"
		from="$(awk -F '[[:space:]]+' '$1 == "FROM" { print $2; exit }' "$df")"
		if ! docker inspect "$from" &> /dev/null; then
			cat >&2 <<-EOF


				warning: '$from' does not exist
				    skipping $dir


			EOF
			echo >&2 "warning, '$from' does not exist; skipping $dir"
			rm -r "$dir/"
		fi
	done
)

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"

for df in */{,*/}Dockerfile; do
	v="$(dirname "$df")"
	if [[ "$v" == *onbuild* ]]; then
		# onbuild needs to be built separately
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
	docker tag "$repo:$tag" "$repo:$fullVersion"
	[ "$v" = "$latest" ] && docker tag "$repo:$tag" "$repo"

	if [ -d "$v/onbuild" ]; then
		docker build -t "$repo:$tag-onbuild" "$v/onbuild"
		docker tag "$repo:$tag-onbuild" "$repo:$fullVersion-onbuild"
		[ "$v" = "$latest" ] && docker tag "$repo:$tag-onbuild" "$repo:onbuild"
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
