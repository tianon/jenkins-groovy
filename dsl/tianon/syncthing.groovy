freeStyleJob('tianon-syncthing') {
	logRotator { numToKeep(5) }
	label('tianon')
	scm {
		git {
			remote {
				url('git@github.com:tianon/docker-syncthing.git')
				credentials('tianon')
				name('origin')
			}
			branches('*/master')
			clean()
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

docker tag -f tianon/syncthing:cli tianon/syncthing-cli
docker tag -f tianon/syncthing:inotify tianon/syncthing-inotify

docker push tianon/syncthing-cli
docker push tianon/syncthing-inotify
""")
	}
}
