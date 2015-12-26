freeStyleJob('crank') {
	logRotator { daysToKeep(30) }
	label('tianon')
	scm {
		git {
			remote { url('https://github.com/paultag/crank.git') }
			branches('*/master')
			clean()
		}
	}
	triggers {
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
docker build -t paultag/crank .
docker run -i --rm \\
	-v "\$HOME/debian/launchpad.asc":/crank/key.asc:ro \\
	paultag/crank eg/gb.hy
""")
	}
}
