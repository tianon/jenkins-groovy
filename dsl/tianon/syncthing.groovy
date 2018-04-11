freeStyleJob('tianon-syncthing') {
	logRotator { numToKeep(5) }
	concurrentBuild(false)
	label('tianon-zoe')
	scm {
		git {
			remote {
				url('git@github.com:tianon/docker-syncthing.git')
				credentials('ssh-git')
				name('origin')
			}
			branches('*/master')
			extensions {
				cleanAfterCheckout()
			}
		}
	}
	triggers {
		cron('H H/6 * * *')
		scm('H/5 * * * *')
	}
	wrappers { colorizeOutput() }
	steps {
		shell("""\
./update.sh

for v in */; do
	v="\${v%/}"
	full="\$(awk '
		\$1 == "ENV" && (\$2 == "SYNCTHING_VERSION" || \$2 == "SYNCTHING_INOTIFY_VERSION") {
			print \$3;
		}
	' "\$v/Dockerfile")"
	if [ "\$full" ]; then
		git commit -m "Update \$v to \$full" -- "\$v/Dockerfile" || true
	fi
done
git push origin HEAD:master || true

./build.sh

./push.sh
""")
	}
}
