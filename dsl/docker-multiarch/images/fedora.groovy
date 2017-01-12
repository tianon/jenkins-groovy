import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)
	if (meta.rhArch == null) {
		// skip unsupported architectures
		continue
	}

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
		triggers {
			cron('H H * * H')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta, ['rhArch']) + '''
sudo rm -rf tmp
mkdir -p tmp

# explicit list of versions we'll attempt to fetch
latest='25' # should match https://github.com/docker-library/official-images/blob/master/library/fedora
versions=(
	25 # https://dl.fedoraproject.org/pub/fedora/linux/releases/25/Docker/x86_64/images/ Fedora-Docker-Base-25-1.3.x86_64.tar.xz
	24 # https://dl.fedoraproject.org/pub/fedora/linux/releases/24/Docker/x86_64/images/ Fedora-Docker-Base-24-1.2.x86_64.tar.xz
	23 # https://dl.fedoraproject.org/pub/fedora/linux/releases/23/Docker/x86_64/ Fedora-Docker-Base-23-20151030.x86_64.tar.xz
	22 # https://dl.fedoraproject.org/pub/fedora/linux/releases/22/Docker/x86_64/ Fedora-Docker-Base-22-20150521.x86_64.tar.xz
	# 22 seems to be the first release to have included "Docker" release artifacts
	# and 24 is the first version that included "fedora-secondary" artifacts (aarch64, ppc64, ppc64le)
)

for v in "${versions[@]}"; do
	for urlBase in \\
		"https://dl.fedoraproject.org/pub/fedora/linux/releases/$v/Docker/$rhArch/images" \\
		"https://dl.fedoraproject.org/pub/fedora-secondary/releases/$v/Docker/$rhArch/images" \\
		"https://dl.fedoraproject.org/pub/fedora/linux/releases/$v/Docker/$rhArch" \\
	; do
		rm -r "tmp/$v"; mkdir "tmp/$v"

		curl -fsSL "$urlBase/?C=M;O=D" -o "tmp/$v/html" || continue

		tarballRegex="Fedora-Docker-Base-$v-[1-9.]+[.]$rhArch[.]tar[.]xz"
		tarballLinks="$(grep -E --only-matching "<a href=\\"$tarballRegex\\">" "tmp/$v/html")" || continue
		[ -n "$tarballLinks" ] || continue

		tarballLink="$(echo "$tarballLinks" | head -n1)"
		tarballName="$(echo "$tarballLink" | sed 's/^<a href="|">$//g')"

		curl -fSL "$urlBase/$tarballName" -o "tmp/$v/save.tar.xz" || continue

		tar -xvf "tmp/$v/save.tar.xz" \\
			--strip-components 1 \\
			--directory "tmp/$v" \\
			--wildcards --wildcards-match-slash \\
			'*/layer.tar' \\
			|| continue
		[ -f "tmp/$v/layer.tar" ] || continue

		{ echo 'FROM scratch'; echo 'ADD layer.tar /'; } > "tmp/$v/Dockerfile"
		docker build -t "$repo:$v" "tmp/$v"
		if [ "$v" = "$latest" ]; then
			docker tag "$repo:$v" "$repo"
		fi
	done
done
''' /* TODO + multiarch.templatePush(meta)*/)
		}
	}
}
