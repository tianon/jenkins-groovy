freeStyleJob('tianon-crank') {
	disabled()
	logRotator { daysToKeep(30) }
	concurrentBuild(false)
	label('tianon-nameless')
	scm {
		git {
			remote { url('https://github.com/paultag/crank.git') }
			branches('*/master')
			extensions {
				cleanAfterCheckout()
			}
		}
	}
	triggers {
		cron('H H/6 * * *')
		scm('H * * * *')
	}
	wrappers { colorizeOutput() }
	steps {
		shell("""\
find eg -name '*.hy' -exec sed -i '
	s!:key.*!:key "0xFDE0F4FE36B4F0C7"!;
	s!:maintainer-name.*!:maintainer-name "Tianon Gravi (Launchpad)"!;
	s!:maintainer-email.*!:maintainer-email "admwiggin+launchpad@gmail.com"!;
' '{}' +
docker build --pull -t paultag/crank .
docker rm -f crank || true
docker run -i --rm \\
	--name crank \\
	-v "\$HOME/debian/launchpad.asc":/crank/key.asc:ro \\
	paultag/crank eg/hy.hy
""")
	}
}
