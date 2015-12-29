def arches = [
	'arm64',
	'armel',
	'armhf',
	'ppc64le',
	's390x',
]

for (arch in arches) {
	freeStyleJob("docker-${arch}-hello") {
		logRotator { daysToKeep(30) }
		label("docker-${arch}")
		scm {
			git {
				remote { url('https://github.com/docker-library/hello-world.git') }
				branches('*/master')
				clean()
			}
		}
		triggers {
			upstream("docker-${arch}-debian", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell("""\
prefix='${arch}'
repo="\$prefix/hello-world"

sed -i "s!^FROM !FROM \$prefix/!; s/nasm/gcc/" Dockerfile.build
sed -i 's/nasm/gcc -static -Os/g; s/\\.asm/\\.c/g' Makefile

# convert hello.asm message contents to C
helloWorldC="\$(awk '
	\$1 == "message:" { yay = 1; next }
	\$1 == "length:" { yay = 0; next }
	yay {
		gsub(/^.*db /, "\\t");
		gsub(/\\"/, "\\\\\\"");
		gsub(/0x0A/, "\\"\\\\n\\"");
		gsub(/, /, " ");
		gsub(/'"'"'/, "\\"");
		print;
	}
' hello.asm)"
cat > hello.c <<EOHWC
#include <stdio.h>

const char *msg =
\$helloWorldC;

int main() {
	printf("%s", msg);
	return 0;
}
EOHWC

./update.sh

docker build -t "\$repo" .

docker run --rm "\$repo"

# we don't have /u/arm64
if [ "\$prefix" != 'arm64' ]; then
	docker images "\$repo" \\
		| awk -F '  +' 'NR>1 { print \$1 ":" \$2 }' \\
		| xargs -rtn1 docker push
fi
""")
		}
	}
}
