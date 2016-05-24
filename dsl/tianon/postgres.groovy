freeStyleJob('tianon-postgres-upgrade') {
	logRotator { numToKeep(5) }
	label('tianon-nameless')
	scm {
		git {
			remote {
				url('git@github.com:tianon/docker-postgres-upgrade.git')
				credentials('tianon')
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
		shell('''\
image='tianon/postgres-upgrade'

rm -rf */
./update.sh
git add . || true
git commit -m 'Apply update.sh' || true
git push origin HEAD:master || true

for combo in */; do
	combo="${combo%/}"
	docker build --pull -t "$image:$combo" "$combo"
	docker push "$image:$combo"
done
''')
	}
}
