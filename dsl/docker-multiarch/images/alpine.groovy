// Alpine only officially supports armhf, x86, x86_64
def arches = [
	//'arm64',
	//'armel',
	'armhf',
	//'ppc64le',
	//'s390x',
]

def apkArches = [
	'amd64': 'x86_64',
	'i386': 'x86',
]

def versions = [
	'v3.3',
	'v3.2',
	'v3.1',
	// 2.x releases don't support armhf
]

for (arch in arches) {
	apkArch = apkArches.containsKey(arch) ? apkArches[arch] : arch

	matrixJob("docker-${arch}-alpine") {
		logRotator { daysToKeep(30) }
		scm {
			git {
				remote {
					url('https://github.com/gliderlabs/docker-alpine.git')
					name('origin')
					refspec('+refs/heads/master:refs/remotes/origin/master')
				}
				branches('*/master')
			}
		}
		triggers {
			cron('H H * * H')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		axes {
			labelExpression('build-host', "docker-${arch}")
			text('version', versions)
		}
		runSequentially() // we can only "docker push" one at a time :(
		steps {
			shell("""\
prefix='${arch}'
apkArch='${apkArch}'

mirror='http://dl-4.alpinelinux.org/alpine'

mkdir -p apk-tools
(
	cd apk-tools
	curl -fSL "\$mirror/\$version/main/\$apkArch/APKINDEX.tar.gz" \
		| tar -xvz
	get_package() {
		local pkg="\$1"
		local ver="\$(awk -F: '\$1 == "P" { pkg = \$2 } pkg == "'"\$pkg"'" && \$1 == "V" { print \$2 }' APKINDEX)"
		curl -fSL "\$mirror/\$version/main/\$apkArch/\$pkg-\$ver.apk" \
			| tar -xvz
	}
	[ -d etc/apk/keys ] || get_package alpine-keys
	[ -x sbin/apk-static ] || get_package apk-tools-static
	[ -x sbin/apk ] || ln -sf apk-static sbin/apk
)
export PATH="\$PATH:\$PWD/apk-tools/sbin"
exec apk --help
sudo rm -rf */rootfs/
git clean -dfx

echo "\$dpkgArch" > arch
echo "\$prefix/debian" > repo
ln -sf ~/docker/docker/contrib/mkimage.sh

maxTries=3
while ! ./update.sh "\$suite"; do
	echo "Update failed; remaining tries: \$(( maxTries - 1 ))"
	if ! (( --maxTries )); then
		(( exitCode++ )) || true
		echo "Update failed; no tries remain; giving up and moving on"
		exit 1
	fi
	sleep 1
done

# we don't have /u/arm64
if [ "\$prefix" != 'arm64' ]; then
	docker push "\$(< repo):\$suite"
	if [ "\$(< latest)" = "\$suite" ]; then
		docker push "\$(< repo):latest"
	fi
fi
""")
		}
	}
}
