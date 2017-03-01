import vars.multiarch

def static opensuseArch(arch) {
	// see http://download.opensuse.org/repositories/Virtualization:/containers:/images:/openSUSE-Tumbleweed/images/
	multiarch.optional([
		'amd64': 'x86_64',
		'arm64': 'aarch64',
		'armhf': 'armv7l',
		'ppc64le': 'ppc64le',
		's390x': 's390x',
	], arch, null)
}

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)
	meta.opensuseArch = opensuseArch(arch)
	if (meta.opensuseArch == null) {
		// skip unsupported architectures
		continue
	}

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		concurrentBuild(false)
		label(meta.label)
		triggers {
			cron('H H * * H')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta, ['opensuseArch']) + '''
sudo rm -rf tmp
mkdir -p tmp

# explicit list of versions we'll attempt to fetch
latest='42.2' # should match https://github.com/docker-library/official-images/blob/master/library/opensuse
versions=(
	42.2
	42.1
	13.2
	Tumbleweed
)

for v in "${versions[@]}"; do
	for urlBase in \\
		"http://download.opensuse.org/repositories/Virtualization:/containers:/images:/openSUSE-${v}/images" \\
	; do
		rm -rf "tmp/$v"; mkdir -p "tmp/$v"

		tarballName="openSUSE-${v}-docker-guest-docker.${opensuseArch}.tar.xz"

		curl -fSL "$urlBase/$tarballName" -o "tmp/$v/rootfs.tar.xz" || continue

		tee "tmp/$v/Dockerfile" <<-'EOD'
			FROM scratch
			ADD rootfs.tar.xz /
		EOD

		tag="repo:${v,,}"
		docker build -t "$tag" "tmp/$v"
		pushImages+=( "$tag" )
		if [ "$v" = "$latest" ]; then
			docker tag "$tag" "$repo:latest"
			pushImages+=( "$repo:latest" )
		fi
	done
done
''' + multiarch.templatePush(meta))
		}
	}
}
