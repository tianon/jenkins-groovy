import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)
	if (meta.apkArch == null) {
		// skip unsupported architectures
		continue
	}

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
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
		steps {
			shell(multiarch.templateArgs(meta, ['apkArch']) + '''
sudo rm -rf tmp
git clean -dfx

# "line 27: MIRROR: parameter null or not set"
rm -r versions/library-2.*

case "$apkArch" in
	armhf)
		# armhf support didn't exist until 3.x
		#rm -r versions/library-2.*
		# 3.1 was rpi-only
		rm -r versions/library-3.1
		;;
esac

optionsFiles=( versions/library-*/options )

# remove "-s" from the BUILD_OPTIONS (we'll specify that explicitly)
sed -i 's! -s ! !g' "${optionsFiles[@]}"

for options in "${optionsFiles[@]}"; do
	(
		source "$options"
		: "${RELEASE:?}" "${MIRROR:?}"
		
		mkdir "apk-tools-$RELEASE"
		cd "apk-tools-$RELEASE"
		curl -fSL "$MIRROR/$RELEASE/main/$apkArch/APKINDEX.tar.gz" \\
			| tar -xvz
		get_package() {
			local pkg="$1"
			local ver="$(awk -F: '$1 == "P" { pkg = $2 } pkg == "'"$pkg"'" && $1 == "V" { print $2 }' APKINDEX)"
			curl -fSL "$MIRROR/$RELEASE/main/$apkArch/$pkg-$ver.apk" \\
				| tar -xvz
		}
		get_package alpine-keys
		get_package apk-tools-static
		ln -sf apk.static sbin/apk
	)
done

# put temporary files in a convenient, known location
mkdir tmp
export TMPDIR="$PWD/tmp"

# this loop is adapted from build()
# see https://github.com/gliderlabs/docker-alpine/blob/9e700b7cbdddf0b95e3786ff8de7ecea8962826c/build#L3-L33
for options in "${optionsFiles[@]}"; do
	(
		dir="$(dirname "$options")"
		
		source "$options"
		: "${TAGS:?}" "${BUILD_OPTIONS:?}" "${RELEASE:?}"
		
		# put "apk" in the PATH
		apkTools="$PWD/apk-tools-$RELEASE"
		export PATH="$PATH:$apkTools/sbin"
		
		# adjust the script to look for /etc/apk/keys in the correct place
		sed "s!/etc/apk/keys!$apkTools/etc/apk/keys!g" builder/scripts/mkimage-alpine.bash > "$dir/mkimage-alpine.bash"
		
		cd "$dir"
		
		chmod +x mkimage-alpine.bash
		sudo PATH="$PATH" \\
			./mkimage-alpine.bash "${BUILD_OPTIONS[@]}"
		for tag in "${TAGS[@]}"; do
			[[ "$tag" == "$image":* ]]
			docker build -t "$prefix/$tag" .
		done
	)
done
''' + multiarch.templatePush(meta))
		}
	}
}
