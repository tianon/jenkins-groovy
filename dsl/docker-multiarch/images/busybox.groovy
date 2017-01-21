import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		concurrentBuild(false)
		label(meta.label)
		scm {
			git {
				remote {
					url('https://github.com/docker-library/busybox.git')
					name('origin')
					refspec('+refs/heads/master:refs/remotes/origin/master')
				}
				branches('*/master')
			}
		}
		triggers {
			upstreams = []
			if (meta.apkArch) {
				upstreams += "docker-${arch}-alpine"
			}
			if (arch == 'armel' || arch == 'armhf') {
				// we build on an arm64 host, and the uclibc build system gets confused in our non-arm64 environment
				upstreams += 'docker-arm64-debian'
			}
			else {
				upstreams += "docker-${arch}-debian"
			}
			upstream(upstreams.join(', '), 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			meta['prefixArm64'] = multiarch.prefix('arm64')
			shell(multiarch.templateArgs(meta, ['dpkgArch', 'gnuArch', 'prefixArm64']) + '''
sed -i "s!^FROM !FROM $prefix/!" */Dockerfile.builder
sed -ri "s!^base='.+'!base='$image:$prefix-'!; s! --pull ! !g" build.sh

# remove some unsupported combinations
case "$dpkgArch" in
	armel)
		# "ARMv4t"
		# https://wiki.debian.org/ArmEabiPort#Choice_of_minimum_CPU
		sed -i 's!BR2_x86_64!BR2_arm BR2_arm922t BR2_ARM_EABI BR2_ARM_SOFT_FLOAT BR2_ARM_INSTRUCTIONS_THUMB!g' uclibc/Dockerfile.builder
		# we build on an arm64 host, and the uclibc build system gets confused in our non-arm64 environment
		sed -i "s!^FROM $prefix/!FROM $prefixArm64/!" uclibc/Dockerfile.builder
		;;

	armhf)
		# "Currently the Debian armhf port requires at least an ARMv7 CPU with Thumb-2 and VFP3D16."
		# https://wiki.debian.org/ArmHardFloatPort#Supported_devices
		sed -i 's!BR2_x86_64!BR2_arm BR2_cortex_a5 BR2_ARM_EABIHF BR2_ARM_FPU_VFPV3D16 BR2_ARM_INSTRUCTIONS_THUMB2!g' uclibc/Dockerfile.builder
		# we build on an arm64 host, and the uclibc build system gets confused in our non-arm64 environment
		sed -i "s!^FROM $prefix/!FROM $prefixArm64/!" uclibc/Dockerfile.builder
		# for some reason, musl on armhf doesn't like optimizing getconf for size
		sed -i 's! -Os ! !g' musl/Dockerfile.builder
		;;

	i386)
		sed -i '/BR2_i386/d; s/BR2_x86_64/BR2_i386/' uclibc/Dockerfile.builder
		;;

	arm64|ppc64el|s390x)
		# TODO determine the possibility and/or breadth of uclibc hacks/config required for these arches
		rm -r uclibc
		;;
esac

if [ -d uclibc ]; then
	# if uclibc still exists, let's force a particular GNU arch for "host-gmp" (otherwise it fails on some arches)
	sed -i 's!^RUN make -C /usr/src/buildroot !&HOST_GMP_CONF_OPTS="--build='"$gnuArch"'" !' uclibc/Dockerfile.builder
fi

(
	set +x
	for df in */Dockerfile.builder; do
		v="$(dirname "$df")"
		from="$(awk '$1 == "FROM" { print $2; exit }' "$df")"
		if ! docker inspect "$from" &> /dev/null; then
			echo >&2 "warning, '$from' does not exist; skipping $v"
			rm -r "$v/"
		fi
	done
)

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"
if [ ! -d "$latest" ]; then
	# if the official "latest" isn't supported, choose another variant
	variants=( */ )
	latest="${variants[0]%/}"
fi

for v in */; do
	v="${v%/}"
	./build.sh "$v"
	docker tag "$image:$prefix-$v" "$repo:$v"
	if [ "$v" = "$latest" ]; then
		docker tag "$repo:$v" "$repo"
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
