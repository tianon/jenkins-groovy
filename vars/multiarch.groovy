package vars

def static allArches(blacklist = []) {
	return [
		'arm64',
		'armel',
		'armhf',
		'i386',
		//'ppc64le',
		's390x',
	] - blacklist
}

def static allNodes(blacklist = []) {
	allArches([
		// run on arm64 (no dedicated worker)
		'armel',
	]).collect { "docker-${it}" } + [
		// extra node responsible for just building Ubuntu
		//'docker-s390x-ubuntu', // thanks to new s390x node, no longer necessary!
	] - blacklist
}

def static optional(list, key, fallback = key) {
	return list.containsKey(key) ? list[key] : fallback
}

def static image(clsName) {
	return optional([
		'buildpack': 'buildpack-deps',
		'hello': 'hello-world',
		'r': 'r-base',
		'rakudo': 'rakudo-star',
	], clsName)
}

def static prefix(arch) {
	return optional([
		'arm64': 'aarch64',
	], arch)
}

def static dpkgArch(arch) {
	return optional([
		'ppc64le': 'ppc64el',
	], arch)
}

def static apkArch(arch) {
	// Alpine only officially supports aarch64 (as of 3.5+), armhf, x86, x86_64
	return optional([
		'amd64': 'x86_64',
		'arm64': 'aarch64',
		'armhf': 'armhf',
		'i386': 'x86',
	], arch, null)
}

def static gnuArch(arch) {
	// "gcc -print-multiarch" / "dpkg-architecture -qDEB_HOST_GNU_TYPE"
	return optional([
		'arm64': 'aarch64-linux-gnu',
		'armel': 'arm-linux-gnueabi',
		'armhf': 'arm-linux-gnueabihf',
		'i386': 'i586-linux-gnu',
		'ppc64le': 'powerpc64le-linux-gnu',
		's390x': 's390x-linux-gnu',
	], arch, 'UNKNOWN')
}

def static rhArch(arch) {
	// https://dl.fedoraproject.org/pub/fedora/linux/releases/25/Docker/
	// https://dl.fedoraproject.org/pub/fedora-secondary/releases/25/Docker/
	// https://fedoraproject.org/wiki/Architectures
	return optional([
		'amd64': 'x86_64',
		'arm64': 'aarch64',
		'armhf': 'armhfp', // ARMv7+
		'ppc64le': 'ppc64le',
		//'armel': 'armsfp', // ARMv5+; not supported for Docker
		//'i386': 'x86', // not supported for Docker
		//'s390x': 's390x', // not supported for Docker (yet?)
	], arch, null)
}

def static archBits(arch) {
	return optional([
		'arm64': '64',
		'armel': '32',
		'armhf': '32',
		'i386': '32',
		'ppc64le': '64',
		's390x': '64',
	], arch, 'unknown')
}

// cls comes from "getClass()"
def static meta(cls, arch) {
	def image = image(cls.name)
	def prefix = prefix(arch)
	def meta = [
		'name': "docker-${arch}-${cls.name}",
		'description': """<a href="https://hub.docker.com/r/${prefix}/${image}/" target="_blank">Docker Hub page (<code>${prefix}/${image}</code>)</a>""",
		'label': "docker-${arch}",
		'prefix': prefix,
		'image': image,
		'arch': arch,
		'dpkgArch': dpkgArch(arch),
		'apkArch': apkArch(arch),
		'gnuArch': gnuArch(arch),
		'rhArch': rhArch(arch),
		'archBits': archBits(arch),
	]
	// thanks to new s390x node, no longer necessary!
	//if (arch == 's390x' && cls.name == 'ubuntu') {
	//	// we have an explicit "Ubuntu" node for s390x now
	//	meta.label += '-ubuntu'
	//}
	return meta
}

def static templateArgs(meta, extra = [], std = ['prefix', 'image', 'repo']) {
	extra = std + extra
	def str = ''
	for (ex in extra) {
		if (ex == 'repo' && !meta.containsKey(ex)) {
			str += ex + '="$prefix/$image"\n'
		}
		else {
			str += "${ex}='${meta[ex]}'\n"
		}
	}
	str += 'pushImages=()\n'
	return str
}
def static templatePush(meta) {
	return '''
if [ "${#pushImages[@]}" -eq 0 ]; then
	IFS=$'\\n'
	pushImages=( $(docker images "$repo" | awk -F '  +' 'NR > 1 && $1 != "<none>" && $2 != "<none>" { print $1 ":" $2 }') )
	unset IFS
fi

pushFailed=
for pushImage in "${pushImages[@]}"; do
	tries=3
	while ! docker push "$pushImage"; do
		if ! (( --tries )); then
			pushFailed=1
			echo "Push of '$pushImage' failed; no tries remain; giving up and moving on"
			break
		fi
		echo "Push of '$pushImage' failed; remaining tries: $tries"
	done
done
[ -z "$pushFailed" ]
'''
}
