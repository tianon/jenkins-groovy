import vars.multiarch

for (arch in multiarch.allArches([
	'armel', // unsupported; upstream considers the arch effectively dead
])) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		concurrentBuild(false)
		label(meta.label)
		scm {
			git {
				remote {
					url('https://github.com/tianon/docker-brew-ubuntu-core.git')
					name('origin')
					refspec('+refs/heads/master:refs/remotes/origin/master')
				}
				branches('*/master')
			}
		}
		triggers {
			cron('H H * * *')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta, ['dpkgArch']) + '''
# delete deprecated suites
# (piping to "cut" because "git clean" will not delete the directory itself if there's a trailing slash)
git ls-files --others --directory '*/' \\
	| cut -d/ -f1 \\
	| xargs --no-run-if-empty git clean -dfx

echo "$dpkgArch" > arch
echo "$repo" > repo
./update.sh

latest="$(< latest)"
for v in */; do
	v="${v%/}"
	if [ ! -f "$v/Dockerfile" ]; then
		continue
	fi
	pushImages+=( "$repo:$v" )
	serial="$(awk -F '=' '$1 == "SERIAL" { print $2; exit }' "$v/build-info.txt")"
	if [ "$serial" ]; then
		pushImages+=( "$repo:$v-$serial" )
	fi
	if [ -s "$v/alias" ]; then
		for a in $(< "$v/alias"); do
			pushImages+=( "$repo:$a" )
		done
	fi
	if [ "$v" = 'latest' ]; then
		pushImages+=( "$repo:latest" )
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
