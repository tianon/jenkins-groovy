import vars.multiarch

for (arch in multiarch.allArches([
	// no upstream binaries available
	'armel',
])) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		concurrentBuild(false)
		label(meta.label)
		scm {
			git {
				remote { url('https://github.com/nodejs/docker-node.git') }
				branches('*/master')
				extensions {
					cleanAfterCheckout()
				}
			}
		}
		triggers {
			upstream("docker-${arch}-buildpack", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta, ['dpkgArch']) + '''
fromArch='linux-x64'
case "$dpkgArch" in
	arm64)   toArch='linux-arm64' ;;
	armhf)   toArch='linux-armv7l' ;;
	i386)    toArch='linux-x86' ;;
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

for v in */; do
	v="${v%/}"
	[ -f "$v/Dockerfile" ] || continue
	fullVersion="$(awk -F '[[:space:]]+' '$1 == "ENV" && $2 == "NODE_VERSION" { print $3; exit }' "$v/Dockerfile")"
	[ -n "$fullVersion" ]
	url="https://nodejs.org/dist/v${fullVersion}/node-v${fullVersion}-${toArch}.tar.xz"
	if ! wget --spider --quiet "$url"; then
		cat >&2 <<-EOF


			warning: $url not found
			    skipping $v


		EOF
		continue
	fi

	docker build -t "$repo:$v" "$v"
	docker tag "$repo:$v" "$repo:$fullVersion"
	[ "$v" = "$latest" ] && docker tag "$repo:$v" "$repo"

	if [ -d "$v/onbuild" ]; then
		docker build -t "$repo:$v-onbuild" "$v/onbuild"
		docker tag "$repo:$v-onbuild" "$repo:$fullVersion-onbuild"
		[ "$v" = "$latest" ] && docker tag "$repo:$v-onbuild" "$repo:onbuild"
	fi

	if [ -d "$v/slim" ]; then
		docker build -t "$repo:$v-slim" "$v/slim"
		docker tag "$repo:$v-slim" "$repo:$fullVersion-slim"
		[ "$v" = "$latest" ] && docker tag "$repo:$v-slim" "$repo:slim"
	fi

	if [ -d "$v/wheezy" ]; then
		docker build -t "$repo:$v-wheezy" "$v/wheezy"
		docker tag "$repo:$v-wheezy" "$repo:$fullVersion-wheezy"
		[ "$v" = "$latest" ] && docker tag "$repo:$v-wheezy" "$repo:wheezy"
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
