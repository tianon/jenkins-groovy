import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
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
			shell(multiarch.templateArgs(meta) + '''
sed -i "s!^FROM !FROM $prefix/!; s/nasm/gcc/" Dockerfile.build
sed -i 's/nasm/gcc -static -Os -nostartfiles -fno-asynchronous-unwind-tables/g; s/\\.asm/\\.c/g' Makefile
sed -i "s/hello-world:build/hello-world:$prefix-build/g" update.sh

# convert hello.asm message contents to C
helloWorldC="$(awk '
	$1 == "message:" { yay = 1; next }
	$1 == "length:" { yay = 0; next }
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
//#include <unistd.h>
#include <sys/syscall.h>

const char msg[] =
$helloWorldC;

void _start() {
	//write(1, msg, sizeof(msg) - 1);
	syscall(SYS_write, 1, msg, sizeof(msg) - 1);
	//_exit(0);
	syscall(SYS_exit, 0);
}
EOHWC

./update.sh

docker build -t "$repo" .

docker images "$repo"

docker run --rm "$repo"
''' + multiarch.templatePush(meta))
		}
	}
}
